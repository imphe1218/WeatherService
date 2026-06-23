package org.weatherservice.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.weatherservice.client.WeatherDownstreamClient;

import org.springframework.stereotype.Service;

@Service
public final class CachedWeatherService {

    private final WeatherDownstreamClient downstreamClient;
    private final Duration freshnessWindow;
    private final Clock clock;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

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

        Object lock = locks.computeIfAbsent(location, key -> new Object());

        synchronized (lock) {
            Instant now = clock.instant();
            CacheEntry cached = cache.get(location);

            if (cached != null && cached.isFresh(now, freshnessWindow)) {
                return cached.value();
            }

            try {
                String value = downstreamClient.fetchWeather(location);
                cache.put(location, new CacheEntry(value, now));
                return value;

            } catch (WebClientRequestException | IllegalStateException ex) {
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
}
