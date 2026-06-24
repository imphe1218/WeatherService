package org.weatherservice.client;

import org.weatherservice.model.WeatherResponse;

@FunctionalInterface
public interface WeatherDownstreamClient {

    reactor.core.publisher.Mono<WeatherResponse> fetchWeather(String location);
}
