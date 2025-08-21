package com.vincent.learning.token.filter;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class ApiAccessLogFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        LoggingWebExchange loggingWebExchange = new LoggingWebExchange(exchange);
        HttpHeaders headers = loggingWebExchange.getResponse().getHeaders();
        headers.add("Cross-Origin-Resource-Policy", "same-origin");
        headers.add("Content-Security-Policy", "default-src 'self'");
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("X-XSS-Protection", "1");
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        return chain.filter(loggingWebExchange)
                .doFinally(
                        signalType -> {
                            loggingWebExchange.getRequest().logRequest();
                            loggingWebExchange.getResponse().logResponse();
                        });
    }
}
