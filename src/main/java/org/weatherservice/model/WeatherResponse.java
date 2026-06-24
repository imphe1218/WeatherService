package org.weatherservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current weather observation returned by the service.")
public record WeatherResponse(
        @Schema(description = "Wind speed reported by the upstream provider.", example = "20.5")
        @JsonProperty("wind_speed") double windSpeed,
        @Schema(description = "Temperature in degrees Celsius.", example = "29.25")
        @JsonProperty("temperature_degrees") double temperatureDegrees) {

}
