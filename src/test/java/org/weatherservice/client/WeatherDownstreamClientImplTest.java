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
import java.util.Map;

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
                        Mono.just("cloudy"));

        WeatherApiProperties properties = new WeatherApiProperties(
                "weatherstack",
                Map.of(
                        "weatherstack", new WeatherApiProperties.Provider(
                                "http://api.weatherstack.com",
                                "/current",
                                Map.of("access_key", "demo-key", "query", "{location}")),
                        "openweathermap", new WeatherApiProperties.Provider(
                                "http://api.openweathermap.org",
                                "/data/2.5/weather",
                                Map.of("q", "{location},AU", "appid", "demo-key"))));

        WeatherDownstreamClient client = new WeatherDownstreamClientImpl(builder, properties);

        MDC.put(TraceContextWebFilter.TRACE_ID_KEY, "trace-123");
        try {
            assertEquals("cloudy", client.fetchWeather("Melbourne").block());
        } finally {
            MDC.remove(TraceContextWebFilter.TRACE_ID_KEY);
        }

        org.mockito.ArgumentCaptor<String> uriCaptor = forClass(String.class);
        verify(requestHeadersUriSpec, times(2)).uri(uriCaptor.capture());
        verify(requestHeadersSpec, times(2)).header(TraceContextWebFilter.TRACE_HEADER, "trace-123");
        assertEquals(2, uriCaptor.getAllValues().size());
        assertTrue(uriCaptor.getAllValues().get(0).contains("api.weatherstack.com/current"));
        assertTrue(uriCaptor.getAllValues().get(1).contains("api.openweathermap.org/data/2.5/weather"));
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
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("sunny"));

        WeatherApiProperties properties = new WeatherApiProperties(
                "weatherstack",
                Map.of("weatherstack", new WeatherApiProperties.Provider(
                        "http://api.weatherstack.com",
                        "/current",
                        Map.of("access_key", "demo-key", "query", "{location}"))));

        WeatherDownstreamClient client = new WeatherDownstreamClientImpl(builder, properties);

        assertEquals("sunny", client.fetchWeather("Melbourne").block());
        org.mockito.ArgumentCaptor<String> uriCaptor = forClass(String.class);
        verify(requestHeadersUriSpec).uri(uriCaptor.capture());
        String requestedUri = uriCaptor.getValue();
        assertTrue(requestedUri.contains("api.weatherstack.com/current"));
        assertTrue(requestedUri.contains("access_key=demo-key"));
        assertTrue(requestedUri.contains("query=Melbourne"));
    }
}
