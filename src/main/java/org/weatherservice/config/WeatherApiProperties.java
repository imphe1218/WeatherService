package org.weatherservice.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather")
public record WeatherApiProperties(String defaultProvider, Map<String, Provider> providers) {

    public WeatherApiProperties {
        defaultProvider = normalizeProviderName(
                defaultProvider == null || defaultProvider.isBlank()
                        ? "weatherstack"
                        : defaultProvider);

        providers = providers == null
                ? Map.of()
                : Collections.unmodifiableMap(providers.entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> normalizeProviderName(entry.getKey()),
                                Map.Entry::getValue,
                                (existing, replacement) -> existing,
                                LinkedHashMap::new)));

        if (!providers.isEmpty() && !providers.containsKey(defaultProvider)) {
            throw new IllegalArgumentException("Default weather provider is not configured: " + defaultProvider);
        }
    }

    public Provider provider(String providerName) {
        String normalizedProviderName = normalizeProviderName(providerName);
        Provider provider = providers.get(normalizedProviderName);

        if (provider == null) {
            throw new IllegalArgumentException("Unknown weather provider: " + providerName);
        }

        return provider;
    }

    @Override
    public Map<String, Provider> providers() {
        return Map.copyOf(providers);
    }

    public List<String> providerPriority() {
        if (providers.isEmpty()) {
            return List.of();
        }

        List<String> priority = new ArrayList<>(providers.size());
        if (providers.containsKey(defaultProvider)) {
            priority.add(defaultProvider);
        }

        for (String providerName : providers.keySet()) {
            if (!providerName.equals(defaultProvider)) {
                priority.add(providerName);
            }
        }

        return List.copyOf(priority);
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
