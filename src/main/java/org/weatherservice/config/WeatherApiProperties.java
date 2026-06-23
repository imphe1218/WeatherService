package org.weatherservice.config;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather")
public record WeatherApiProperties(Map<String, Provider> providers) {

    public WeatherApiProperties {
        providers = providers == null
                ? Map.of()
                : providers.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        entry -> normalizeProviderName(entry.getKey()),
                        Map.Entry::getValue));
    }

    public Provider provider(String providerName) {
        String normalizedProviderName = normalizeProviderName(providerName);
        Provider provider = providers.get(normalizedProviderName);

        if (provider == null) {
            throw new IllegalArgumentException("Unknown weather provider: " + providerName);
        }

        return provider;
    }

    private static String normalizeProviderName(String providerName) {
        return Objects.requireNonNull(providerName, "providerName")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    public record Provider(String baseUrl, String path, Map<String, String> queryParams) {

        public Provider {
            queryParams = queryParams == null ? Map.of() : Map.copyOf(queryParams);
        }
    }
}
