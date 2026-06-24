package org.weatherservice.client;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.weatherservice.config.TraceContextWebFilter;
import org.weatherservice.config.WeatherApiProperties;
import org.weatherservice.logging.LogSanitizer;

@Service
public final class WeatherDownstreamClientImpl implements WeatherDownstreamClient {

    private static final Logger log = LoggerFactory.getLogger(WeatherDownstreamClientImpl.class);

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
        String logLocation = LogSanitizer.value(resolvedLocation);
        RuntimeException lastFailure = null;

        for (String providerName : properties.providerPriority()) {
            String logProvider = LogSanitizer.value(providerName);
            try {
                if (log.isInfoEnabled()) {
                    log.info("Trying downstream provider={} location={}", logProvider, logLocation);
                }
                return fetchFromProvider(providerName, resolvedLocation);
            } catch (IllegalStateException ex) {
                if (log.isWarnEnabled()) {
                    log.warn("Downstream provider failed provider={} location={} cause={}",
                            logProvider,
                            logLocation,
                            LogSanitizer.value(ex.toString()));
                }
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
        String traceId = MDC.get(TraceContextWebFilter.TRACE_ID_KEY);

        try {
            WebClient.RequestHeadersSpec<?> request = webClient.get().uri(uri);
            if (traceId != null && !traceId.isBlank()) {
                request = request.header(TraceContextWebFilter.TRACE_HEADER, traceId);
            }

            String response = request.retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) {
                throw new IllegalStateException("Weather provider returned an empty response: " + providerName);
            }

            if (log.isInfoEnabled()) {
                log.info("Downstream provider succeeded provider={} location={}",
                        LogSanitizer.value(providerName),
                        LogSanitizer.value(location));
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
