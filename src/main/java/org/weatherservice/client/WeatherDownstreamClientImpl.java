package org.weatherservice.client;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.weatherservice.config.WeatherApiProperties;

@Service
public class WeatherDownstreamClientImpl implements WeatherDownstreamClient {

    private final WebClient webClient;
    private final WeatherApiProperties properties;

    public WeatherDownstreamClientImpl(WebClient.Builder webClientBuilder, WeatherApiProperties properties) {
        this.webClient = Objects.requireNonNull(webClientBuilder, "webClientBuilder").build();
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public String fetchWeather(String provider, String location) {
        Objects.requireNonNull(location, "location");

        WeatherApiProperties.Provider providerConfig = properties.provider(provider);
        String resolvedLocation = location.trim();
        String uri = buildUri(providerConfig, resolvedLocation);

        return Objects.requireNonNull(
                webClient.get()
                        .uri(uri)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(),
                "downstream weather response");
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
