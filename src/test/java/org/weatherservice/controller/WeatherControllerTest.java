package org.weatherservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.weatherservice.service.CachedWeatherService;

class WeatherControllerTest {

    @Test
    void returnsWeatherFromTheCachedService() {
        CachedWeatherService weatherService = new CachedWeatherService(
                (provider, location) -> provider + ":" + location,
                Duration.ofSeconds(3),
                Clock.systemUTC());
        WeatherController controller = new WeatherController(weatherService);

        assertEquals("weatherstack:Melbourne",
                controller.getWeather("Melbourne", "weatherstack").getBody());
    }

    @Test
    void rejectsBlankLocation() {
        CachedWeatherService weatherService = new CachedWeatherService(
                (provider, location) -> provider + ":" + location,
                Duration.ofSeconds(3),
                Clock.systemUTC());
        WeatherController controller = new WeatherController(weatherService);

        assertThrows(ResponseStatusException.class, () -> controller.getWeather(" ", "weatherstack"));
    }
}
