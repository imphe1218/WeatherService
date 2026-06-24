package org.weatherservice.config;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.weatherservice.logging.LogSanitizer;

import reactor.core.publisher.Mono;

@Component
@SuppressWarnings("PMD.LawOfDemeter")
public final class TraceContextWebFilter implements WebFilter {

    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_KEY = "traceId";

    private static final Logger log = LoggerFactory.getLogger(TraceContextWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = resolveTraceId(exchange.getRequest());
        ServerWebExchange tracedExchange = exchange.mutate()
                .request(mutateRequest(exchange.getRequest(), traceId))
                .build();

        tracedExchange.getResponse().getHeaders().set(TRACE_HEADER, traceId);

        return Mono.defer(() -> {
            MDC.put(TRACE_ID_KEY, traceId);
            if (log.isInfoEnabled()) {
                log.info("Request started method={} path={}",
                        LogSanitizer.value(exchange.getRequest().getMethod()),
                        LogSanitizer.value(exchange.getRequest().getURI().getPath()));
            }

            return chain.filter(tracedExchange)
                    .doFinally(signalType -> {
                        if (log.isInfoEnabled()) {
                            log.info("Request completed status={} signal={}",
                                    LogSanitizer.value(tracedExchange.getResponse().getStatusCode()),
                                    LogSanitizer.value(signalType));
                        }
                        MDC.remove(TRACE_ID_KEY);
                    });
        });
    }

    private static ServerHttpRequest mutateRequest(ServerHttpRequest request, String traceId) {
        return request.mutate()
                .header(TRACE_HEADER, traceId)
                .build();
    }

    private static String resolveTraceId(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(TRACE_HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }

        return UUID.randomUUID().toString();
    }
}
