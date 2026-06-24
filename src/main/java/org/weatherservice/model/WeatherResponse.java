package org.weatherservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherResponse(
        @JsonProperty("wind_speed") double windSpeed,
        @JsonProperty("temperature_degrees") double temperatureDegrees) {

}
