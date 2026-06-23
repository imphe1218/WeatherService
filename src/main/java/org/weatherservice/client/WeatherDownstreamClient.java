package org.weatherservice.client;

@FunctionalInterface
public interface WeatherDownstreamClient {

    String fetchWeather(String provider, String location);
}
