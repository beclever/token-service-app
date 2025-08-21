package com.vincent.learning.token.filter;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;

@Slf4j
public class LoggingRequestDecorator extends ServerHttpRequestDecorator {

    private final ServerHttpRequest delegate;
    private final StringBuilder body = new StringBuilder();

    public LoggingRequestDecorator(ServerHttpRequest delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    /**
     * it works for @ReqeustBody, not for form data
     *
     * @return
     */
    @Override
    public Flux<DataBuffer> getBody() {
        return super.getBody().doOnNext(this::capture);
    }

    private void capture(DataBuffer buffer) {
        this.body.append(buffer.toString(StandardCharsets.UTF_8));
    }

    public void logRequest() {
        if (log.isDebugEnabled()) {
            String method = Optional.ofNullable(delegate.getMethod()).orElse(HttpMethod.GET).name();
            String headers = delegate.getHeaders().toString();
            log.debug(
                    "\n------Request------\n>> {} {}\n>> Headers: {}\n>> Body: {}\n-------------------",
                    method,
                    delegate.getURI(),
                    headers,
                    body);
        }
    }
}
