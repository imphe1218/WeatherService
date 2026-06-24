package org.weatherservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.weatherservice.client.WeatherDownstreamClient;
import org.weatherservice.model.WeatherResponse;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CachedWeatherServiceTest {

    @Mock
    private WeatherDownstreamClient downstreamClient;

    @Mock
    private Clock clock;

    @Test
    void returnsCachedValueWithinThreeSecondsWithoutCallingDownstreamAgain() {
        Instant initial = Instant.parse("2026-06-21T00:00:00Z");
        when(clock.instant()).thenReturn(initial, initial);
        WeatherResponse response = new WeatherResponse(20.5, 29.25);
        when(downstreamClient.fetchWeather("Shanghai")).thenReturn(Mono.just(response));

        CachedWeatherService service = new CachedWeatherService(downstreamClient, Duration.ofSeconds(3), clock);

        assertEquals(response, service.getWeather("Shanghai").block());
        assertEquals(response, service.getWeather("Shanghai").block());
        verify(downstreamClient, times(1)).fetchWeather("Shanghai");
        verifyNoMoreInteractions(downstreamClient);
    }

    @Test
    void treatsCachedValueAsFreshAtExactlyThreeSeconds() {
        Instant initial = Instant.parse("2026-06-21T00:00:00Z");
        when(clock.instant()).thenReturn(initial, initial.plusSeconds(3));
        WeatherResponse response = new WeatherResponse(20.5, 29.25);
        when(downstreamClient.fetchWeather("Shanghai")).thenReturn(Mono.just(response));

        CachedWeatherService service = new CachedWeatherService(downstreamClient, Duration.ofSeconds(3), clock);

        assertEquals(response, service.getWeather("Shanghai").block());
        assertEquals(response, service.getWeather("Shanghai").block());
        verify(downstreamClient, times(1)).fetchWeather("Shanghai");
        verifyNoMoreInteractions(downstreamClient);
    }

    @Test
    void fallsBackToCachedValueWhenAllProvidersAreDownAfterFreshnessExpires() {
        Instant initial = Instant.parse("2026-06-21T00:00:00Z");
        when(clock.instant()).thenReturn(initial, initial.plusSeconds(4));
        WeatherResponse response = new WeatherResponse(20.5, 29.25);
        when(downstreamClient.fetchWeather("Shanghai"))
                .thenReturn(Mono.just(response))
                .thenReturn(Mono.error(new IllegalStateException("all configured providers are down")));

        CachedWeatherService service = new CachedWeatherService(downstreamClient, Duration.ofSeconds(3), clock);

        assertEquals(response, service.getWeather("Shanghai").block());
        assertEquals(response, service.getWeather("Shanghai").block());
        verify(downstreamClient, times(2)).fetchWeather("Shanghai");
        verifyNoMoreInteractions(downstreamClient);
    }

    @Test
    void failsWhenAllProvidersAreDownAndThereIsNoCachedValueYet() {
        Instant initial = Instant.parse("2026-06-21T00:00:00Z");
        when(clock.instant()).thenReturn(initial);
        when(downstreamClient.fetchWeather("Shanghai"))
                .thenReturn(Mono.error(new IllegalStateException("all configured providers are down")));

        CachedWeatherService service = new CachedWeatherService(downstreamClient, Duration.ofSeconds(3), clock);

        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> service.getWeather("Shanghai").block());

        assertEquals("Downstream weather service is unavailable and no cached value exists.",
                exception.getMessage());
        verify(downstreamClient, times(1)).fetchWeather("Shanghai");
        verifyNoMoreInteractions(downstreamClient);
    }

    @Test
    void cachesEachLocationSeparately() {
        Instant initial = Instant.parse("2026-06-21T00:00:00Z");
        when(clock.instant()).thenReturn(initial, initial);
        WeatherResponse response = new WeatherResponse(20.5, 29.25);
        when(downstreamClient.fetchWeather("Shanghai")).thenReturn(Mono.just(response));
        when(downstreamClient.fetchWeather("Melbourne")).thenReturn(Mono.just(response));

        CachedWeatherService service = new CachedWeatherService(downstreamClient, Duration.ofSeconds(3), clock);

        assertEquals(response, service.getWeather("Shanghai").block());
        assertEquals(response, service.getWeather("Melbourne").block());
        verify(downstreamClient, times(1)).fetchWeather("Shanghai");
        verify(downstreamClient, times(1)).fetchWeather("Melbourne");
        verifyNoMoreInteractions(downstreamClient);
    }

    @Test
    void normalizesCacheKeyButKeepsTrimmedLocationForDownstreamCall() {
        Instant initial = Instant.parse("2026-06-21T00:00:00Z");
        when(clock.instant()).thenReturn(initial, initial);
        WeatherResponse response = new WeatherResponse(20.5, 29.25);
        when(downstreamClient.fetchWeather("Melbourne")).thenReturn(Mono.just(response));

        CachedWeatherService service = new CachedWeatherService(downstreamClient, Duration.ofSeconds(3), clock);

        assertEquals(response, service.getWeather(" Melbourne ").block());
        assertEquals(response, service.getWeather("melbourne").block());
        verify(downstreamClient, times(1)).fetchWeather("Melbourne");
        verifyNoMoreInteractions(downstreamClient);
    }
}
