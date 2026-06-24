package org.weatherservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class WeatherServiceConfigTest {

    @Test
    void freshnessWindowUsesConfiguredCacheTtl() {
        WeatherServiceConfig config = new WeatherServiceConfig();

        assertEquals(Duration.ofSeconds(10), config.freshnessWindow(Duration.ofSeconds(10)));
    }

    @Test
    void freshnessWindowDefaultsWhenCacheTtlIsNotPositive() {
        WeatherServiceConfig config = new WeatherServiceConfig();

        assertEquals(Duration.ofSeconds(3), config.freshnessWindow(Duration.ZERO));
    }
}
