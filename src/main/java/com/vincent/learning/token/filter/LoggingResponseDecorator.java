package com.vincent.learning.token.filter;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class LoggingResponseDecorator extends ServerHttpResponseDecorator {
    private final ServerHttpResponse delegate;
    private final StringBuilder body = new StringBuilder();

    public LoggingResponseDecorator(ServerHttpResponse delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        Flux<DataBuffer> buffer = Flux.from(body);
        return super.writeWith(buffer.doOnNext(this::capture));
    }

    private void capture(DataBuffer buffer) {
        this.body.append(buffer.toString(StandardCharsets.UTF_8));
    }

    public void logResponse() {
        if (log.isDebugEnabled() && delegate != null) {
            log.debug(
                    "\n------Response------\n>> {} \n>> Headers: {}\n>> Body: {}\n-------------------",
                    delegate.getStatusCode(),
                    delegate.getHeaders(),
                    body);
        }
    }
}
