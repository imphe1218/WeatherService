package org.weatherservice.service;

public class WeatherServiceException extends RuntimeException {

    @java.io.Serial
    static final long serialVersionUID = -7034897190745766001L;

    public WeatherServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public WeatherServiceException(String message) {
        super(message);
    }
}
