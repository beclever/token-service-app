package com.vincent.learning.token.filter;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;

public class LoggingWebExchange extends ServerWebExchangeDecorator {
    private final LoggingRequestDecorator requestDecorator;
    private final LoggingResponseDecorator responseDecorator;

    protected LoggingWebExchange(ServerWebExchange delegate) {
        super(delegate);
        requestDecorator = new LoggingRequestDecorator(delegate.getRequest());
        responseDecorator = new LoggingResponseDecorator(delegate.getResponse());
    }

    @Override
    public LoggingRequestDecorator getRequest() {
        return requestDecorator;
    }

    @Override
    public LoggingResponseDecorator getResponse() {
        return responseDecorator;
    }
}
