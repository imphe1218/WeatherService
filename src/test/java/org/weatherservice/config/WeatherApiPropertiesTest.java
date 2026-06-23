package org.weatherservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

class WeatherApiPropertiesTest {

    @Test
    void providerLookupIsCaseInsensitive() {
        WeatherApiProperties properties = new WeatherApiProperties(Map.of(
                "weatherstack", new WeatherApiProperties.Provider(
                        "http://api.weatherstack.com",
                        "/current",
                        Map.of("access_key", "demo-key", "query", "{location}")),
                "openweathermap", new WeatherApiProperties.Provider(
                        "http://api.openweathermap.org",
                        "/data/2.5/weather",
                        Map.of("appid", "demo-key", "q", "{location},AU"))));

        assertEquals("http://api.weatherstack.com", properties.provider("WEATHERSTACK").baseUrl());
        assertEquals("/data/2.5/weather", properties.provider("openweathermap").path());
    }

    @Test
    void unknownProviderIsRejected() {
        WeatherApiProperties properties = new WeatherApiProperties(Map.of());

        assertThrows(IllegalArgumentException.class, () -> properties.provider("missing"));
    }
}
