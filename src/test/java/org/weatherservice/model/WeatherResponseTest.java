package org.weatherservice.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class WeatherResponseTest {

    @Test
    void serializesToTheApiResponseShape() throws JsonProcessingException {
        String json = new ObjectMapper().writeValueAsString(new WeatherResponse(20.5, 29.25));

        assertEquals("{\"wind_speed\":20.5,\"temperature_degrees\":29.25}", json);
    }
}
