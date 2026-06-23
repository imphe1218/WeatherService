package org.weatherservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.weatherservice.client.WeatherDownstreamClient;

import reactor.core.publisher.Mono;

class WeatherServiceConfigTest {

    private final WeatherServiceConfig config = new WeatherServiceConfig();

    private final WebClientConfig webClientConfig = new WebClientConfig();

    @Test
    void clockAndFreshnessWindowHaveExpectedDefaults() {
        Clock clock = config.clock();
        Duration freshnessWindow = config.freshnessWindow();

        assertEquals(ZoneOffset.UTC, clock.getZone());
        assertEquals(Duration.ofSeconds(3), freshnessWindow);
    }

    @Test
    void weatherDownstreamClientUsesTheConfiguredWebClient() {
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

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:8081")
                .exchangeFunction(exchangeFunction)
                .build();

        WeatherDownstreamClient client = config.weatherDownstreamClient(webClient);

        assertEquals("sunny", client.fetchWeather("Shanghai"));
        assertEquals("/weather", requestedPath.get());
        assertEquals("location=Shanghai", requestedQuery.get());
    }

    @Test
    void webClientConfigBuildsABaseUrlAwareClient() {
        WebClient webClient = webClientConfig.webClient(WebClient.builder(), "http://example.com");

        assertNotNull(webClient);
    }
}
