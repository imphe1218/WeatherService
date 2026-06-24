package org.weatherservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WeatherApiPropertiesTest {

    @Test
    void providerLookupIsCaseInsensitiveAndDefaultProviderIsPrioritized() {
        WeatherApiProperties properties = new WeatherApiProperties(
                "weatherstack",
                Map.of(
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
        assertEquals(List.of("weatherstack", "openweathermap"), properties.providerPriority());
    }

    @Test
    void defaultProviderMustBeConfigured() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherApiProperties(
                "weatherstack",
                Map.of("openweathermap", new WeatherApiProperties.Provider(
                        "http://api.openweathermap.org",
                        "/data/2.5/weather",
                        Map.of("appid", "demo-key", "q", "{location},AU")))));
    }
}
