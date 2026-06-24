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
        when(downstreamClient.fetchWeather("Shanghai")).thenReturn("sunny");

        CachedWeatherService service = new CachedWeatherService(downstreamClient, Duration.ofSeconds(3), clock);

        assertEquals("sunny", service.getWeather("Shanghai"));
        assertEquals("sunny", service.getWeather("Shanghai"));
        verify(downstreamClient, times(1)).fetchWeather("Shanghai");
        verifyNoMoreInteractions(downstreamClient);
    }

    @Test
    void fallsBackToCachedValueWhenAllProvidersAreDownAfterFreshnessExpires() {
        Instant initial = Instant.parse("2026-06-21T00:00:00Z");
        when(clock.instant()).thenReturn(initial, initial.plusSeconds(4));
        when(downstreamClient.fetchWeather("Shanghai"))
                .thenReturn("sunny")
                .thenThrow(new IllegalStateException("all configured providers are down"));

        CachedWeatherService service = new CachedWeatherService(downstreamClient, Duration.ofSeconds(3), clock);

        assertEquals("sunny", service.getWeather("Shanghai"));
        assertEquals("sunny", service.getWeather("Shanghai"));
        verify(downstreamClient, times(2)).fetchWeather("Shanghai");
        verifyNoMoreInteractions(downstreamClient);
    }

    @Test
    void failsWhenAllProvidersAreDownAndThereIsNoCachedValueYet() {
        Instant initial = Instant.parse("2026-06-21T00:00:00Z");
        when(clock.instant()).thenReturn(initial);
        when(downstreamClient.fetchWeather("Shanghai"))
                .thenThrow(new IllegalStateException("all configured providers are down"));

        CachedWeatherService service = new CachedWeatherService(downstreamClient, Duration.ofSeconds(3), clock);

        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> service.getWeather("Shanghai"));

        assertEquals("Downstream weather service is unavailable and no cached value exists.",
                exception.getMessage());
        verify(downstreamClient, times(1)).fetchWeather("Shanghai");
        verifyNoMoreInteractions(downstreamClient);
    }

    @Test
    void cachesEachLocationSeparately() {
        Instant initial = Instant.parse("2026-06-21T00:00:00Z");
        when(clock.instant()).thenReturn(initial, initial);
        when(downstreamClient.fetchWeather("Shanghai")).thenReturn("sunny");
        when(downstreamClient.fetchWeather("Melbourne")).thenReturn("sunny");

        CachedWeatherService service = new CachedWeatherService(downstreamClient, Duration.ofSeconds(3), clock);

        assertEquals("sunny", service.getWeather("Shanghai"));
        assertEquals("sunny", service.getWeather("Melbourne"));
        verify(downstreamClient, times(1)).fetchWeather("Shanghai");
        verify(downstreamClient, times(1)).fetchWeather("Melbourne");
        verifyNoMoreInteractions(downstreamClient);
    }
}
