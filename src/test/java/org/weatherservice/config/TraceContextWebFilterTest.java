package org.weatherservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

class TraceContextWebFilterTest {

    @Test
    void usesExistingTraceHeaderAndAddsItToResponse() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/v1/weather")
                .header(TraceContextWebFilter.TRACE_HEADER, "trace-123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<String> mdcTraceId = new AtomicReference<>();
        AtomicReference<String> requestTraceId = new AtomicReference<>();

        WebFilterChain chain = tracedExchange -> {
            mdcTraceId.set(MDC.get(TraceContextWebFilter.TRACE_ID_KEY));
            requestTraceId.set(tracedExchange.getRequest().getHeaders().getFirst(TraceContextWebFilter.TRACE_HEADER));
            return Mono.empty();
        };

        new TraceContextWebFilter().filter(exchange, chain).block();

        assertEquals("trace-123", mdcTraceId.get());
        assertEquals("trace-123", requestTraceId.get());
        assertEquals("trace-123", exchange.getResponse().getHeaders().getFirst(TraceContextWebFilter.TRACE_HEADER));
        assertNull(MDC.get(TraceContextWebFilter.TRACE_ID_KEY));
    }
}
