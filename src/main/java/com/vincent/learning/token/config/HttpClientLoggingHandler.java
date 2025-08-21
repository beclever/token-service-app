package com.vincent.learning.token.config;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LoggingHandler;
import java.nio.charset.StandardCharsets;

public class HttpClientLoggingHandler extends LoggingHandler {

    @Override
    protected String format(ChannelHandlerContext ctx, String event, Object arg) {
        if (arg instanceof ByteBuf msg) {
            return msg.toString(StandardCharsets.UTF_8);
        }
        return super.format(ctx, event, arg);
    }
}
