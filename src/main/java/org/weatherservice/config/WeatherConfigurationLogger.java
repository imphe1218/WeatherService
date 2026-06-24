package org.weatherservice.config;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.weatherservice.logging.LogSanitizer;

@Component
public final class WeatherConfigurationLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WeatherConfigurationLogger.class);

    private final WeatherApiProperties properties;
    private final String basePath;
    private final String resourcePath;
    private final String cityParam;

    public WeatherConfigurationLogger(
            WeatherApiProperties properties,
            @Value("${spring.webflux.base-path:/v1}") String basePath,
            @Value("${weather.api.resource-path:/weather}") String resourcePath,
            @Value("${weather.api.city-param:city}") String cityParam) {

        this.properties = properties;
        this.basePath = basePath;
        this.resourcePath = resourcePath;
        this.cityParam = cityParam;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (log.isInfoEnabled()) {
            log.info("Weather API endpoint basePath={} resourcePath={} cityParam={}",
                    LogSanitizer.value(basePath),
                    LogSanitizer.value(resourcePath),
                    LogSanitizer.value(cityParam));
        }

        if (log.isInfoEnabled()) {
            log.info("Weather provider priority={}", LogSanitizer.value(properties.providerPriority()));
        }

        if (log.isInfoEnabled()) {
            log.info("Weather provider circuitBreaker failureThreshold={} openDuration={}",
                    LogSanitizer.value(properties.circuitBreaker().failureThreshold()),
                    LogSanitizer.value(properties.circuitBreaker().openDuration()));
        }

        for (String providerName : properties.providerPriority()) {
            WeatherApiProperties.Provider provider = properties.provider(providerName);
            if (log.isInfoEnabled()) {
                log.info("Weather provider configured provider={} baseUrl={} path={} queryParams={} credentialStatus={}",
                        LogSanitizer.value(providerName),
                        LogSanitizer.value(provider.baseUrl()),
                        LogSanitizer.value(provider.path()),
                        LogSanitizer.value(nonSensitiveQueryParamNames(provider.queryParams())),
                        LogSanitizer.value(credentialStatus(provider.queryParams())));
            }
        }
    }

    private static String nonSensitiveQueryParamNames(Map<String, String> queryParams) {
        return queryParams.keySet().stream()
                .filter(paramName -> !isSensitiveParam(paramName))
                .sorted()
                .collect(Collectors.joining(","));
    }

    private static String credentialStatus(Map<String, String> queryParams) {
        return queryParams.entrySet().stream()
                .filter(entry -> isSensitiveParam(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + resolvedStatus(entry.getValue()))
                .sorted()
                .collect(Collectors.joining(","));
    }

    private static boolean isSensitiveParam(String paramName) {
        String normalized = paramName.toLowerCase(Locale.ROOT);
        return normalized.contains("key")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("appid");
    }

    private static String resolvedStatus(String value) {
        if (value == null || value.isBlank()) {
            return "missing";
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("YOUR_")) {
            return "default-placeholder";
        }

        return "configured";
    }
}
