package org.weatherservice.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Map;

import org.junit.jupiter.api.Test;

class WeatherConfigurationLoggerTest {

    @Test
    void logsResolvedConfigurationWithoutExposingSecretValues() {
        WeatherApiProperties properties = new WeatherApiProperties(
                "weatherstack",
                null,
                Map.of(
                        "weatherstack", new WeatherApiProperties.Provider(
                                "http://api.weatherstack.com",
                                "/current",
                                Map.of("access_key", "YOUR_ACCESS_KEY", "query", "{location}")),
                        "openweathermap", new WeatherApiProperties.Provider(
                                "http://api.openweathermap.org",
                                "/data/2.5/weather",
                                Map.of("appid", "real-key", "q", "{location},AU"))));

        WeatherConfigurationLogger logger = new WeatherConfigurationLogger(
                properties,
                "/v1",
                "/weather",
                "city");

        assertDoesNotThrow(() -> logger.run(null));
    }
}
