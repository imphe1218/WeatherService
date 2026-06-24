package org.weatherservice.client;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.weatherservice.config.TraceContextWebFilter;
import org.weatherservice.config.WeatherApiProperties;
import org.weatherservice.logging.LogSanitizer;
import org.weatherservice.model.WeatherResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public final class WeatherDownstreamClientImpl implements WeatherDownstreamClient {

    private static final Logger log = LoggerFactory.getLogger(WeatherDownstreamClientImpl.class);
    private static final double KILOMETERS_PER_HOUR_PER_METER_PER_SECOND = 3.6;

    private final WebClient webClient;
    private final WeatherApiProperties properties;
    private final ObjectMapper objectMapper;
    private final ProviderCircuitBreaker circuitBreaker;

    @Autowired
    public WeatherDownstreamClientImpl(
            WebClient.Builder webClientBuilder,
            WeatherApiProperties properties,
            ObjectMapper objectMapper) {
        this(
                webClientBuilder,
                properties,
                objectMapper,
                new ProviderCircuitBreaker(Objects.requireNonNull(properties, "properties").circuitBreaker()));
    }

    WeatherDownstreamClientImpl(
            WebClient.Builder webClientBuilder,
            WeatherApiProperties properties,
            ObjectMapper objectMapper,
            ProviderCircuitBreaker circuitBreaker) {
        this.webClient = Objects.requireNonNull(webClientBuilder, "webClientBuilder").build();
        this.properties = Objects.requireNonNull(properties, "properties");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker");
    }

    @Override
    public Mono<WeatherResponse> fetchWeather(String location) {
        Objects.requireNonNull(location, "location");

        String resolvedLocation = location.trim();
        String logLocation = LogSanitizer.value(resolvedLocation);
        AtomicReference<Throwable> lastFailure = new AtomicReference<>();

        return Flux.fromIterable(properties.providerPriority())
                .concatMap(providerName -> {
                    String logProvider = LogSanitizer.value(providerName);
                    if (!circuitBreaker.isCallPermitted(providerName)) {
                        ProviderCircuitBreakerLogger.logCircuitOpen(providerName, resolvedLocation);
                        lastFailure.set(new IllegalStateException(
                                "Weather provider circuit breaker is open: " + providerName));
                        return Mono.empty();
                    }

                    logProviderAttempt(logProvider, logLocation);
                    return fetchFromProvider(providerName, resolvedLocation)
                            .doOnNext(response -> circuitBreaker.recordSuccess(providerName))
                            .onErrorResume(IllegalStateException.class, ex -> {
                                ProviderCircuitBreaker.CircuitBreakerFailure circuitFailure =
                                        circuitBreaker.recordFailure(providerName, ex);
                                logProviderFailure(logProvider, logLocation, ex);
                                ProviderCircuitBreakerLogger.logCircuitOpened(providerName, circuitFailure);
                                lastFailure.set(ex);
                                return Mono.empty();
                            });
                })
                .next()
                .switchIfEmpty(Mono.defer(() -> {
                    Throwable failure = lastFailure.get();
                    if (failure == null) {
                        return Mono.error(new IllegalStateException("No weather providers are configured."));
                    }
                    return Mono.error(new IllegalStateException(
                            "All configured weather providers are unavailable.",
                            failure));
                }));
    }

    private Mono<WeatherResponse> fetchFromProvider(String providerName, String location) {
        WeatherApiProperties.Provider providerConfig = properties.provider(providerName);
        String uri = buildUri(providerConfig, location);
        String traceId = MDC.get(TraceContextWebFilter.TRACE_ID_KEY);

        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(uri);
        if (traceId != null && !traceId.isBlank()) {
            request = request.header(TraceContextWebFilter.TRACE_HEADER, traceId);
        }

        return request.retrieve()
                .bodyToMono(String.class)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Weather provider returned an empty response: " + providerName)))
                .map(response -> {
                    WeatherMetrics metrics = extractMetrics(providerName, response)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Weather provider response is missing required metrics: " + providerName));
                    logWeatherMetrics(providerName, location, metrics);
                    if (log.isInfoEnabled()) {
                        log.info("Downstream provider succeeded provider={} location={}",
                                LogSanitizer.value(providerName),
                                LogSanitizer.value(location));
                    }
                    return metrics.toResponse();
                })
                .onErrorMap(
                        org.springframework.web.reactive.function.client.WebClientRequestException.class,
                        ex -> new IllegalStateException("Weather provider is unavailable: " + providerName, ex))
                .onErrorMap(
                        org.springframework.web.reactive.function.client.WebClientResponseException.class,
                        ex -> new IllegalStateException("Weather provider is unavailable: " + providerName, ex));
    }

    private static void logWeatherMetrics(String providerName, String location, WeatherMetrics metrics) {
        if (log.isInfoEnabled()) {
            log.info("Downstream weather metrics provider={} location={} temperatureCelsius={} windSpeed={} windSpeedUnit={}",
                    LogSanitizer.value(providerName),
                    LogSanitizer.value(location),
                    LogSanitizer.value(metrics.temperatureCelsius()),
                    LogSanitizer.value(metrics.windSpeed()),
                    LogSanitizer.value(metrics.windSpeedUnit()));
        }
    }

    private Optional<WeatherMetrics> extractMetrics(String providerName, String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return switch (providerName) {
                case "weatherstack" -> extractWeatherstackMetrics(root);
                case "openweathermap" -> extractOpenWeatherMapMetrics(root);
                default -> Optional.empty();
            };
        } catch (IOException ex) {
            logMetricExtractionFailure(providerName, ex);
            return Optional.empty();
        }
    }

    private static Optional<WeatherMetrics> extractWeatherstackMetrics(JsonNode root) {
        JsonNode current = root.path("current");
        JsonNode temperature = current.path("temperature");
        JsonNode windSpeed = current.path("wind_speed");

        if (!temperature.isNumber() || !windSpeed.isNumber()) {
            return Optional.empty();
        }

        return Optional.of(new WeatherMetrics(
                temperature.asDouble(),
                windSpeed.asDouble(),
                "km/h"));
    }

    private static Optional<WeatherMetrics> extractOpenWeatherMapMetrics(JsonNode root) {
        JsonNode temperature = root.path("main").path("temp");
        JsonNode windSpeed = root.path("wind").path("speed");

        if (!temperature.isNumber() || !windSpeed.isNumber()) {
            return Optional.empty();
        }

        return Optional.of(new WeatherMetrics(
                temperature.asDouble(),
                windSpeed.asDouble() * KILOMETERS_PER_HOUR_PER_METER_PER_SECOND,
                "km/h"));
    }

    private static void logMetricExtractionFailure(String providerName, IOException ex) {
        if (log.isWarnEnabled()) {
            log.warn("Downstream weather metrics unavailable provider={} cause={}",
                    LogSanitizer.value(providerName),
                    LogSanitizer.value(ex.toString()));
        }
    }

    private static void logProviderAttempt(String providerName, String location) {
        if (log.isInfoEnabled()) {
            log.info("Trying downstream provider={} location={}", providerName, location);
        }
    }

    private static void logProviderFailure(String providerName, String location, RuntimeException ex) {
        if (log.isWarnEnabled()) {
            log.warn("Downstream provider failed provider={} location={} cause={}",
                    providerName,
                    location,
                    LogSanitizer.value(ex.toString()));
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

    private record WeatherMetrics(double temperatureCelsius, double windSpeed, String windSpeedUnit) {

        private WeatherResponse toResponse() {
            return new WeatherResponse(windSpeed, temperatureCelsius);
        }

    }
}
