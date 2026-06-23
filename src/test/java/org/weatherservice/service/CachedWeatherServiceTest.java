
package org.weatherservice.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.weatherservice.client.WeatherDownstreamClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CachedWeatherServiceTest {

    @Test
    void returnsCachedValueWithinThreeSecondsWithoutCallingDownstreamAgain() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-21T00:00:00Z"));
        CountingClient client = new CountingClient();
        CachedWeatherService service = new CachedWeatherService(client, Duration.ofSeconds(3), clock);

        assertEquals("weatherstack:Shanghai", service.getWeather("weatherstack", "Shanghai"));
        assertEquals("weatherstack:Shanghai", service.getWeather("weatherstack", "Shanghai"));
        assertEquals(1, client.calls());
    }

    @Test
    void fallsBackToCachedValueWhenDownstreamIsDownAfterFreshnessExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-21T00:00:00Z"));
        FlakyClient client = new FlakyClient();
        CachedWeatherService service = new CachedWeatherService(client, Duration.ofSeconds(3), clock);

        assertEquals("sunny", service.getWeather("weatherstack", "Shanghai"));
        clock.advance(Duration.ofSeconds(4));

        assertEquals("sunny", service.getWeather("weatherstack", "Shanghai"));
        assertEquals(2, client.calls());
    }

    @Test
    void failsWhenDownstreamIsDownAndThereIsNoCachedValueYet() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-21T00:00:00Z"));
        AlwaysFailClient client = new AlwaysFailClient();
        CachedWeatherService service = new CachedWeatherService(client, Duration.ofSeconds(3), clock);

        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> service.getWeather("weatherstack", "Shanghai"));

        assertEquals("Downstream weather service is unavailable and no cached value exists.",
                exception.getMessage());
        assertEquals(1, client.calls());
    }

    @Test
    void cachesDifferentProvidersSeparately() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-21T00:00:00Z"));
        CountingClient client = new CountingClient();
        CachedWeatherService service = new CachedWeatherService(client, Duration.ofSeconds(3), clock);

        assertEquals("weatherstack:Shanghai", service.getWeather("weatherstack", "Shanghai"));
        assertEquals("openweathermap:Shanghai", service.getWeather("openweathermap", "Shanghai"));
        assertEquals(2, client.calls());
    }

    private static final class CountingClient implements WeatherDownstreamClient {

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public String fetchWeather(String provider, String location) {
            calls.incrementAndGet();
            return provider + ":" + location;
        }

        int calls() {
            return calls.get();
        }
    }

    private static final class FlakyClient implements WeatherDownstreamClient {

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public String fetchWeather(String provider, String location) {
            calls.incrementAndGet();
            if (calls.get() == 1) {
                return "sunny";
            }
            throw new IllegalStateException("downstream unavailable");
        }

        int calls() {
            return calls.get();
        }
    }

    private static final class AlwaysFailClient implements WeatherDownstreamClient {

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public String fetchWeather(String provider, String location) {
            calls.incrementAndGet();
            throw new IllegalStateException("downstream unavailable");
        }

        int calls() {
            return calls.get();
        }
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
