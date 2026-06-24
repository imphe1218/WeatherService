package org.weatherservice.client;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.weatherservice.config.WeatherApiProperties;

@Service
public final class WeatherDownstreamClientImpl implements WeatherDownstreamClient {

    private final WebClient webClient;
    private final WeatherApiProperties properties;

    public WeatherDownstreamClientImpl(WebClient.Builder webClientBuilder, WeatherApiProperties properties) {
        this.webClient = Objects.requireNonNull(webClientBuilder, "webClientBuilder").build();
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public String fetchWeather(String location) {
        Objects.requireNonNull(location, "location");

        String resolvedLocation = location.trim();
        RuntimeException lastFailure = null;

        for (String providerName : properties.providerPriority()) {
            try {
                return fetchFromProvider(providerName, resolvedLocation);
            } catch (IllegalStateException ex) {
                lastFailure = ex;
            }
        }

        if (lastFailure == null) {
            throw new IllegalStateException("No weather providers are configured.");
        }

        throw new IllegalStateException("All configured weather providers are unavailable.", lastFailure);
    }

    private String fetchFromProvider(String providerName, String location) {
        WeatherApiProperties.Provider providerConfig = properties.provider(providerName);
        String uri = buildUri(providerConfig, location);

        try {
            String response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) {
                throw new IllegalStateException("Weather provider returned an empty response: " + providerName);
            }

            return response;
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException
                 | org.springframework.web.reactive.function.client.WebClientResponseException ex) {
            throw new IllegalStateException("Weather provider is unavailable: " + providerName, ex);
        }
    }

    private String buildUri(WeatherApiProperties.Provider providerConfig, String location) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(providerConfig.baseUrl())
                .path(providerConfig.path());

        for (Map.Entry<String, String> queryParam : providerConfig.queryParams().entrySet()) {
            uriBuilder.queryParam(queryParam.getKey(), queryParam.getValue().replace("{location}", location));
        }

        return uriBuilder.build().encode().toUriString();
    }
}
