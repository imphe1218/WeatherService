package org.weatherservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.weatherservice.logging.LogSanitizer;
import org.weatherservice.service.CachedWeatherService;

@RestController
@RequestMapping("${weather.api.resource-path:/weather}")
public class WeatherController {

    private static final Logger log = LoggerFactory.getLogger(WeatherController.class);

    private final CachedWeatherService weatherService;

    public WeatherController(CachedWeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public ResponseEntity<String> getWeather(
            @RequestParam(name = "${weather.api.city-param:city}") String city) {

        if (city == null || city.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "city is required");
        }

        try {
            if (log.isInfoEnabled()) {
                log.info("Received weather request city={}", LogSanitizer.value(city.trim()));
            }
            return ResponseEntity.ok(weatherService.getWeather(city));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
