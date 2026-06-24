package org.weatherservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weatherservice.logging.LogSanitizer;

final class ProviderCircuitBreakerLogger {

    private static final Logger log = LoggerFactory.getLogger(WeatherDownstreamClientImpl.class);

    private ProviderCircuitBreakerLogger() {

    }

    static void logCircuitOpen(String providerName, String location) {
        if (log.isWarnEnabled()) {
            log.warn("Circuit breaker open provider={} location={} action=skip-downstream-call",
                    LogSanitizer.value(providerName),
                    LogSanitizer.value(location));
        }
    }

    static void logCircuitOpened(
            String providerName,
            ProviderCircuitBreaker.CircuitBreakerFailure circuitFailure) {

        if (circuitFailure.opened() && log.isWarnEnabled()) {
            log.warn("Circuit breaker opened provider={}",
                    LogSanitizer.value(providerName));
        }
    }
}
