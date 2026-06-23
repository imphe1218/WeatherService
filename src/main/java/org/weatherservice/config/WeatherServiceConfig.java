package org.weatherservice.config;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.weatherservice.client.WeatherDownstreamClient;

@Configuration(proxyBeanMethods = false)
public class WeatherServiceConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public Duration freshnessWindow() {
        return Duration.ofSeconds(3);
    }

    @Bean
    public WeatherDownstreamClient weatherDownstreamClient(WebClient webClient) {
        return location -> Objects.requireNonNull(
                webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/weather")
                                .queryParam("location", location)
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(),
                "downstream weather response");
    }
}
