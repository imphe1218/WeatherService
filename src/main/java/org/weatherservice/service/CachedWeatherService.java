package org.weatherservice.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.weatherservice.client.WeatherDownstreamClient;
import org.weatherservice.logging.LogSanitizer;
import org.weatherservice.model.WeatherResponse;

import reactor.core.publisher.Mono;

@Service
public final class CachedWeatherService {

    private static final Logger log = LoggerFactory.getLogger(CachedWeatherService.class);

    private final WeatherDownstreamClient downstreamClient;
    private final Duration freshnessWindow;
    private final Clock clock;

    private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<CacheKey, Mono<WeatherResponse>> refreshesInFlight = new ConcurrentHashMap<>();

    public CachedWeatherService(
            WeatherDownstreamClient downstreamClient,
            Duration freshnessWindow,
            Clock clock) {

        this.downstreamClient = Objects.requireNonNull(downstreamClient, "downstreamClient");
        this.freshnessWindow = Objects.requireNonNull(freshnessWindow, "freshnessWindow");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Mono<WeatherResponse> getWeather(String location) {
        Objects.requireNonNull(location, "location");

        return Mono.defer(() -> getWeatherOnSubscription(location));
    }

    private Mono<WeatherResponse> getWeatherOnSubscription(String location) {

        String normalizedLocation = location.trim();
        String logLocation = LogSanitizer.value(normalizedLocation);
        CacheKey cacheKey = new CacheKey(normalizedLocation.toLowerCase(java.util.Locale.ROOT));
        Instant now = clock.instant();
        CacheEntry cached = cache.get(cacheKey);

        if (cached != null && cached.isFresh(now, freshnessWindow)) {
            logCacheHit(logLocation);
            return Mono.just(cached.value());
        }

        CacheEntry staleCache = cached;
        Instant fetchedAt = now;

        return refreshesInFlight.computeIfAbsent(cacheKey, key ->
                refreshCache(key, normalizedLocation, logLocation, staleCache, fetchedAt));
    }

    private Mono<WeatherResponse> refreshCache(
            CacheKey cacheKey,
            String normalizedLocation,
            String logLocation,
            CacheEntry staleCache,
            Instant fetchedAt) {

        logCacheRefresh(logLocation);

        return downstreamClient.fetchWeather(normalizedLocation)
                .doOnNext(value -> {
                    cache.put(cacheKey, new CacheEntry(value, fetchedAt));
                })
                .onErrorResume(IllegalStateException.class, ex -> {
                    if (staleCache != null) {
                        logStaleCacheFallback(logLocation, ex);
                        return Mono.just(staleCache.value());
                    }
                    logNoCacheFallback(logLocation, ex);
                    return Mono.error(new WeatherServiceException(
                            "Downstream weather service is unavailable and no cached value exists.",
                            ex
                    ));
                })
                .doFinally(signalType -> refreshesInFlight.remove(cacheKey))
                .cache();
    }

    private static void logCacheHit(String location) {
        if (log.isInfoEnabled()) {
            log.info("Cache hit location={}", location);
        }
    }

    private static void logCacheRefresh(String location) {
        if (log.isInfoEnabled()) {
            log.info("Cache stale or empty location={} refreshing from downstream", location);
        }
    }

    private static void logStaleCacheFallback(String location, RuntimeException ex) {
        if (log.isWarnEnabled()) {
            log.warn("Downstream unavailable location={} serving stale cache cause={}",
                    location,
                    LogSanitizer.value(ex.toString()));
        }
    }

    private static void logNoCacheFallback(String location, RuntimeException ex) {
        if (log.isErrorEnabled()) {
            log.error("Downstream unavailable location={} and no cached value exists cause={}",
                    location,
                    LogSanitizer.value(ex.toString()));
        }
    }

    private record CacheEntry(WeatherResponse value, Instant fetchedAt) {

        private boolean isFresh(Instant now, Duration freshnessWindow) {
            return !fetchedAt.plus(freshnessWindow).isBefore(now);
        }
    }

    private record CacheKey(String location) {

    }
}
