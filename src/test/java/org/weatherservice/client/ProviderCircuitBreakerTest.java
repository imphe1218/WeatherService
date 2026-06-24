package org.weatherservice.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.weatherservice.config.WeatherApiProperties;

class ProviderCircuitBreakerTest {

    @Test
    void opensAfterFailureThresholdAndPermitsHalfOpenCallAfterOpenDuration() throws InterruptedException {
        ProviderCircuitBreaker circuitBreaker = new ProviderCircuitBreaker(
                new WeatherApiProperties.CircuitBreaker(2, Duration.ofMillis(10)));

        assertTrue(circuitBreaker.isCallPermitted("weatherstack"));

        circuitBreaker.recordFailure("weatherstack", new IllegalStateException("downstream failure"));
        assertTrue(circuitBreaker.isCallPermitted("weatherstack"));

        circuitBreaker.recordFailure("weatherstack", new IllegalStateException("downstream failure"));
        assertFalse(circuitBreaker.isCallPermitted("weatherstack"));

        Thread.sleep(20);
        assertTrue(circuitBreaker.isCallPermitted("weatherstack"));
    }

    @Test
    void successResetsRecordedFailures() {
        ProviderCircuitBreaker circuitBreaker = new ProviderCircuitBreaker(
                new WeatherApiProperties.CircuitBreaker(2, Duration.ofSeconds(30)));

        assertTrue(circuitBreaker.isCallPermitted("weatherstack"));
        circuitBreaker.recordFailure("weatherstack", new IllegalStateException("downstream failure"));
        assertTrue(circuitBreaker.isCallPermitted("weatherstack"));
        circuitBreaker.recordSuccess("weatherstack");

        assertTrue(circuitBreaker.isCallPermitted("weatherstack"));
        circuitBreaker.recordFailure("weatherstack", new IllegalStateException("downstream failure"));
        assertTrue(circuitBreaker.isCallPermitted("weatherstack"));
    }
}
