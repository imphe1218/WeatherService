package org.weatherservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.weatherservice.model.WeatherResponse;
import org.weatherservice.service.CachedWeatherService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("${weather.api.resource-path:/weather}")
@Tag(name = "Weather", description = "Weather lookup operations")
public class WeatherController {

    private static final Logger log = LoggerFactory.getLogger(WeatherController.class);

    private final CachedWeatherService weatherService;

    public WeatherController(CachedWeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    @Operation(
            summary = "Get weather by city",
            description = "Returns the current cached weather observation for the requested city.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Weather observation returned",
                        content = @Content(schema = @Schema(implementation = WeatherResponse.class))),
                @ApiResponse(responseCode = "400", description = "City parameter is missing or blank")
            })
    public Mono<ResponseEntity<WeatherResponse>> getWeather(
            @Parameter(
                    name = "city",
                    description = "City name to look up.",
                    example = "Melbourne",
                    required = true)
            @RequestParam(name = "${weather.api.city-param:city}") String city) {

        if (city == null || city.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "city is required");
        }

        try {
            if (log.isInfoEnabled()) {
                log.info("Received weather request city={}", LogSanitizer.value(city.trim()));
            }
            return weatherService.getWeather(city).map(ResponseEntity::ok);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
