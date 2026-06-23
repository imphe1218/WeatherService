package org.weatherservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.weatherservice.service.CachedWeatherService;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private static final String DEFAULT_PROVIDER = "weatherstack";

    private final CachedWeatherService weatherService;

    public WeatherController(CachedWeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public ResponseEntity<String> getWeather(
            @RequestParam String location,
            @RequestParam(name = "provider", defaultValue = DEFAULT_PROVIDER) String provider) {

        if (location == null || location.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "location is required");
        }

        try {
            return ResponseEntity.ok(weatherService.getWeather(provider, location));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
