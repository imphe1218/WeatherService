package org.weatherservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.weatherservice.model.WeatherResponse;
import org.weatherservice.service.CachedWeatherService;

import reactor.core.publisher.Mono;

class WeatherControllerTest {

    @Test
    void returnsWeatherFromTheCachedService() {
        WeatherResponse response = new WeatherResponse(20.5, 29.25);
        CachedWeatherService weatherService = new CachedWeatherService(
                location -> Mono.just(response),
                Duration.ofSeconds(3),
                Clock.systemUTC());
        WeatherController controller = new WeatherController(weatherService);

        assertEquals(response, controller.getWeather("Melbourne").block().getBody());
    }

    @Test
    void rejectsBlankLocation() {
        CachedWeatherService weatherService = new CachedWeatherService(
                location -> Mono.just(new WeatherResponse(20.5, 29.25)),
                Duration.ofSeconds(3),
                Clock.systemUTC());
        WeatherController controller = new WeatherController(weatherService);

        assertThrows(ResponseStatusException.class, () -> controller.getWeather(" "));
    }
}
