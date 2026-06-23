package org.weatherservice;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class WeatherServiceApplicationTest {

    @Test
    void mainStartsWithNonWebApplicationType() {
        assertDoesNotThrow(() ->
                WeatherServiceApplication.main(new String[] {"--spring.main.web-application-type=none"}));
    }
}
