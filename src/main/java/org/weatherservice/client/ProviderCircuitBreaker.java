package org.weatherservice.client;

import java.util.Objects;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.weatherservice.config.WeatherApiProperties;

final class ProviderCircuitBreaker {

    private static final float OPEN_AFTER_ALL_RECORDED_CALLS_FAIL = 100.0F;
    private static final int HALF_OPEN_TRIAL_CALLS = 1;

    private final CircuitBreakerRegistry registry;

    ProviderCircuitBreaker(WeatherApiProperties.CircuitBreaker properties) {
        WeatherApiProperties.CircuitBreaker resolvedProperties = Objects.requireNonNull(properties, "properties");
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(OPEN_AFTER_ALL_RECORDED_CALLS_FAIL)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(resolvedProperties.failureThreshold())
                .minimumNumberOfCalls(resolvedProperties.failureThreshold())
                .waitDurationInOpenState(resolvedProperties.openDuration())
                .permittedNumberOfCallsInHalfOpenState(HALF_OPEN_TRIAL_CALLS)
                .build();

        this.registry = CircuitBreakerRegistry.of(config);
    }

    boolean isCallPermitted(String providerName) {
        return circuitBreaker(providerName).tryAcquirePermission();
    }

    CircuitBreakerFailure recordFailure(String providerName, RuntimeException exception) {
        CircuitBreaker circuitBreaker = circuitBreaker(providerName);
        circuitBreaker.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, exception);

        return new CircuitBreakerFailure(!circuitBreaker.tryAcquirePermission());
    }

    void recordSuccess(String providerName) {
        circuitBreaker(providerName).onSuccess(0, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    private CircuitBreaker circuitBreaker(String providerName) {
        return registry.circuitBreaker(providerName);
    }

    record CircuitBreakerFailure(boolean opened) {
    }
}
