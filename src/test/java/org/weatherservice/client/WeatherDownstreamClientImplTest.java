package org.weatherservice.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.weatherservice.config.WeatherApiProperties;

import reactor.core.publisher.Mono;

class WeatherDownstreamClientImplTest {

    @Test
    void buildsWeatherstackUriFromProperties() {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        AtomicReference<String> requestedQuery = new AtomicReference<>();

        ExchangeFunction exchangeFunction = request -> {
            requestedPath.set(request.url().getPath());
            requestedQuery.set(request.url().getQuery());
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body("sunny")
                    .build());
        };

        WeatherApiProperties properties = new WeatherApiProperties(Map.of(
                "weatherstack", new WeatherApiProperties.Provider(
                        "http://api.weatherstack.com",
                        "/current",
                        Map.of("access_key", "demo-key", "query", "{location}"))));

        WeatherDownstreamClient client = new WeatherDownstreamClientImpl(
                WebClient.builder().exchangeFunction(exchangeFunction),
                properties);

        assertEquals("sunny", client.fetchWeather("weatherstack", "Melbourne"));
        assertEquals("/current", requestedPath.get());
        assertTrue(requestedQuery.get().contains("access_key=demo-key"));
        assertTrue(requestedQuery.get().contains("query=Melbourne"));
    }

    @Test
    void buildsOpenWeatherUriFromProperties() {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        AtomicReference<String> requestedQuery = new AtomicReference<>();

        ExchangeFunction exchangeFunction = request -> {
            requestedPath.set(request.url().getPath());
            requestedQuery.set(request.url().getQuery());
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body("cloudy")
                    .build());
        };

        WeatherApiProperties properties = new WeatherApiProperties(Map.of(
                "openweathermap", new WeatherApiProperties.Provider(
                        "http://api.openweathermap.org",
                        "/data/2.5/weather",
                        Map.of("q", "{location},AU", "appid", "demo-key"))));

        WeatherDownstreamClient client = new WeatherDownstreamClientImpl(
                WebClient.builder().exchangeFunction(exchangeFunction),
                properties);

        assertEquals("cloudy", client.fetchWeather("openweathermap", "Melbourne"));
        assertEquals("/data/2.5/weather", requestedPath.get());
        assertTrue(requestedQuery.get().contains("appid=demo-key"));
        assertTrue(requestedQuery.get().contains("q=Melbourne"));
    }
}
