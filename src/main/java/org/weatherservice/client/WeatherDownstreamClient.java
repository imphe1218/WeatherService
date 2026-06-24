package org.weatherservice.client;

@FunctionalInterface
public interface WeatherDownstreamClient {

    reactor.core.publisher.Mono<String> fetchWeather(String location);
}
