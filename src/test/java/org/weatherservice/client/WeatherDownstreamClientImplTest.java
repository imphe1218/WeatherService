package org.weatherservice.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.weatherservice.config.TraceContextWebFilter;
import org.weatherservice.config.WeatherApiProperties;
import org.weatherservice.model.WeatherResponse;

import reactor.core.publisher.Mono;

class WeatherDownstreamClientImplTest {

    @Test
    void usesDefaultProviderAndFallsBackToOpenWeatherWhenWeatherstackFails() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        Builder builder = mock(Builder.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        WebClient webClient = mock(WebClient.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        RequestHeadersUriSpec requestHeadersUriSpec = mock(RequestHeadersUriSpec.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        RequestHeadersSpec requestHeadersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        when(builder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(WebClientResponseException.create(
                        503,
                        "Service Unavailable",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8)),
                        Mono.just("{\"main\":{\"temp\":18.5},\"wind\":{\"speed\":4.1}}"));

        WeatherApiProperties properties = new WeatherApiProperties(
                "weatherstack",
                null,
                Map.of(
                        "weatherstack", new WeatherApiProperties.Provider(
                                "http://api.weatherstack.com",
                                "/current",
                                Map.of("access_key", "demo-key", "query", "{location}")),
                        "openweathermap", new WeatherApiProperties.Provider(
                                "http://api.openweathermap.org",
                                "/data/2.5/weather",
                                Map.of("q", "{location},AU", "units", "metric", "appid", "demo-key"))));

        WeatherDownstreamClient client = new WeatherDownstreamClientImpl(builder, properties, new ObjectMapper());

        MDC.put(TraceContextWebFilter.TRACE_ID_KEY, "trace-123");
        try {
            assertEquals(new WeatherResponse(14.76, 18.5), client.fetchWeather("Melbourne").block());
        } finally {
            MDC.remove(TraceContextWebFilter.TRACE_ID_KEY);
        }

        org.mockito.ArgumentCaptor<String> uriCaptor = forClass(String.class);
        verify(requestHeadersUriSpec, times(2)).uri(uriCaptor.capture());
        verify(requestHeadersSpec, times(2)).header(TraceContextWebFilter.TRACE_HEADER, "trace-123");
        assertEquals(2, uriCaptor.getAllValues().size());
        assertTrue(uriCaptor.getAllValues().get(0).contains("api.weatherstack.com/current"));
        assertTrue(uriCaptor.getAllValues().get(1).contains("api.openweathermap.org/data/2.5/weather"));
        assertTrue(uriCaptor.getAllValues().get(1).contains("units=metric"));
    }

    @Test
    void buildsWeatherstackUriFromProperties() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        Builder builder = mock(Builder.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        WebClient webClient = mock(WebClient.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        RequestHeadersUriSpec requestHeadersUriSpec = mock(RequestHeadersUriSpec.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        RequestHeadersSpec requestHeadersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        when(builder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just("{\"current\":{\"temperature\":21,\"wind_speed\":13}}"));

        WeatherApiProperties properties = new WeatherApiProperties(
                "weatherstack",
                null,
                Map.of("weatherstack", new WeatherApiProperties.Provider(
                        "http://api.weatherstack.com",
                        "/current",
                        Map.of("access_key", "demo-key", "query", "{location}"))));

        WeatherDownstreamClient client = new WeatherDownstreamClientImpl(builder, properties, new ObjectMapper());

        assertEquals(new WeatherResponse(13.0, 21.0), client.fetchWeather("Melbourne").block());
        org.mockito.ArgumentCaptor<String> uriCaptor = forClass(String.class);
        verify(requestHeadersUriSpec).uri(uriCaptor.capture());
        String requestedUri = uriCaptor.getValue();
        assertTrue(requestedUri.contains("api.weatherstack.com/current"));
        assertTrue(requestedUri.contains("access_key=demo-key"));
        assertTrue(requestedUri.contains("query=Melbourne"));
    }

    @Test
    void skipsProviderAfterCircuitBreakerOpensAndContinuesToFallbackProvider() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        Builder builder = mock(Builder.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        WebClient webClient = mock(WebClient.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        RequestHeadersUriSpec requestHeadersUriSpec = mock(RequestHeadersUriSpec.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        RequestHeadersSpec requestHeadersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        when(builder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(providerUnavailable(),
                        Mono.just("{\"main\":{\"temp\":18.5},\"wind\":{\"speed\":4.1}}"),
                        providerUnavailable(),
                        Mono.just("{\"main\":{\"temp\":19.5},\"wind\":{\"speed\":5.1}}"),
                        Mono.just("{\"main\":{\"temp\":20.5},\"wind\":{\"speed\":6.1}}"));

        WeatherApiProperties properties = new WeatherApiProperties(
                "weatherstack",
                new WeatherApiProperties.CircuitBreaker(2, Duration.ofMinutes(1)),
                Map.of(
                        "weatherstack", new WeatherApiProperties.Provider(
                                "http://api.weatherstack.com",
                                "/current",
                                Map.of("access_key", "demo-key", "query", "{location}")),
                        "openweathermap", new WeatherApiProperties.Provider(
                                "http://api.openweathermap.org",
                                "/data/2.5/weather",
                                Map.of("q", "{location},AU", "units", "metric", "appid", "demo-key"))));
        ProviderCircuitBreaker circuitBreaker = new ProviderCircuitBreaker(
                properties.circuitBreaker());
        WeatherDownstreamClient client = new WeatherDownstreamClientImpl(
                builder,
                properties,
                new ObjectMapper(),
                circuitBreaker);

        assertEquals(new WeatherResponse(14.76, 18.5), client.fetchWeather("Melbourne").block());
        assertEquals(new WeatherResponse(18.36, 19.5), client.fetchWeather("Melbourne").block());
        assertEquals(new WeatherResponse(21.96, 20.5), client.fetchWeather("Melbourne").block());

        org.mockito.ArgumentCaptor<String> uriCaptor = forClass(String.class);
        verify(requestHeadersUriSpec, times(5)).uri(uriCaptor.capture());
        long weatherstackCalls = uriCaptor.getAllValues().stream()
                .filter(uri -> uri.contains("api.weatherstack.com/current"))
                .count();
        long openWeatherMapCalls = uriCaptor.getAllValues().stream()
                .filter(uri -> uri.contains("api.openweathermap.org/data/2.5/weather"))
                .count();

        assertEquals(2, weatherstackCalls);
        assertEquals(3, openWeatherMapCalls);
    }

    private static Mono<String> providerUnavailable() {
        return Mono.error(WebClientResponseException.create(
                503,
                "Service Unavailable",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8));
    }
}
