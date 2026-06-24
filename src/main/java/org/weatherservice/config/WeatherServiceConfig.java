package org.weatherservice.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class WeatherServiceConfig {

    private static final Duration DEFAULT_FRESHNESS_WINDOW = Duration.ofSeconds(3);

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public Duration freshnessWindow(@Value("${weather.cache.ttl:3s}") Duration cacheTtl) {
        if (cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero()) {
            return DEFAULT_FRESHNESS_WINDOW;
        }

        return cacheTtl;
    }
}
