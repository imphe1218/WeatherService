package org.weatherservice.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.weatherservice.client.WeatherDownstreamClient;

import org.springframework.stereotype.Service;

@Service
public final class CachedWeatherService {

    private final WeatherDownstreamClient downstreamClient;
    private final Duration freshnessWindow;
    private final Clock clock;

    private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<CacheKey, Object> locks = new ConcurrentHashMap<>();

    public CachedWeatherService(
            WeatherDownstreamClient downstreamClient,
            Duration freshnessWindow,
            Clock clock) {

        this.downstreamClient = Objects.requireNonNull(downstreamClient, "downstreamClient");
        this.freshnessWindow = Objects.requireNonNull(freshnessWindow, "freshnessWindow");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public String getWeather(String location) {
        Objects.requireNonNull(location, "location");

        String normalizedLocation = location.trim();
        CacheKey cacheKey = new CacheKey(normalizedLocation.toLowerCase(java.util.Locale.ROOT));
        Object lock = locks.computeIfAbsent(cacheKey, key -> new Object());

        synchronized (lock) {
            Instant now = clock.instant();
            CacheEntry cached = cache.get(cacheKey);

            if (cached != null && cached.isFresh(now, freshnessWindow)) {
                return cached.value();
            }

            try {
                String value = downstreamClient.fetchWeather(normalizedLocation);
                cache.put(cacheKey, new CacheEntry(value, now));
                return value;

            } catch (IllegalStateException ex) {
                if (cached != null) {
                    return cached.value();
                }
                throw new WeatherServiceException(
                        "Downstream weather service is unavailable and no cached value exists.",
                        ex
                );
            }
        }
    }

    private record CacheEntry(String value, Instant fetchedAt) {

        private boolean isFresh(Instant now, Duration freshnessWindow) {
            return !fetchedAt.plus(freshnessWindow).isBefore(now);
        }
    }

    private record CacheKey(String location) {

    }
}
