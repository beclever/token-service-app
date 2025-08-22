package com.vincent.learning.token.filter;

import com.ericsson.iot.model.util.Op;
import org.apache.commons.io.IOUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class LogBodyFilter extends CommonsRequestLoggingFilter {

    private static final ArrayList<String> IGNORE_URLS = new ArrayList<>(Arrays.asList("/v1/readiness"));
    @Override
    protected boolean isIncludeQueryString() {
        return true;
    }

    @Override
    public boolean isIncludeHeaders() {
        return true;
    }

    @Override
    protected boolean isIncludePayload() {
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        boolean isFirstRequest = !isAsyncDispatch(request);
        HttpServletRequest requestToUse = request;
        HttpServletResponse responseToUse = response;

        boolean shouldLog = shouldLog(requestToUse) && !Op.has(IGNORE_URLS, requestToUse.getRequestURI());

        if(shouldLog){
            if (isIncludePayload() && isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
                requestToUse = new PreReadRequestWrapper(request);
            }

            if (isIncludePayload() && isFirstRequest && !(response instanceof ContentCachingResponseWrapper)) {
                responseToUse = new ContentCachingResponseWrapper(response);
            }


            if (isFirstRequest) {
                logger.debug(createMessage(requestToUse));
            }
        }

        try {
            filterChain.doFilter(requestToUse, responseToUse);
        }finally {
            if(shouldLog) {
                log(responseToUse);
            }
        }
    }

    private void log(HttpServletResponse responseToUse) throws IOException {
        if (responseToUse instanceof ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(responseToUse, ContentCachingResponseWrapper.class);
            if(Op.notEmpty(wrapper)){
                logger.debug(createMessage(wrapper));
                wrapper.copyBodyToResponse();
            }
        }
    }

    protected String createMessage(HttpServletRequest request) {
        StringBuilder msg = new StringBuilder();
        msg.append("\n>>>>>>Request>>>>>>\n>> ");

        msg.append(request.getMethod()).append(" ").append(request.getRequestURI());

        if (isIncludeQueryString()) {
            String queryString = request.getQueryString();
            if (Op.notEmpty(queryString)) {
                msg.append('?').append(queryString);
            }
        }

        if (isIncludeHeaders()) {
            HttpHeaders httpHeaders = new ServletServerHttpRequest(request).getHeaders();
            if(httpHeaders.get("authorization") != null){
                httpHeaders.put("authorization", Collections.singletonList("omit..."));
            }
            msg.append("\n>> Headers: ").append(httpHeaders);
        }

        if (isIncludePayload()) {
            PreReadRequestWrapper wrapper = WebUtils.getNativeRequest(request, PreReadRequestWrapper.class);
            if (wrapper != null) {
                String body = wrapper.getBody();
                if (body.length() > 0) {
                    msg.append("\n>> Body: ").append(wrapper.getBody());
                }
            }
        }

        msg.append("\n>>>>>>>>>>>>>>>>>>>");

        return msg.toString();
    }

    protected String createMessage(ContentCachingResponseWrapper response) throws UnsupportedEncodingException {
        StringBuilder msg = new StringBuilder();
        msg.append("\n<<<<<<Response<<<<<<<\n<< ");

        msg.append(response.getStatus()).append(" ").append(HttpStatus.valueOf(response.getStatus()).getReasonPhrase());

        if (isIncludePayload()) {
            String body = new String(response.getContentAsByteArray(), response.getCharacterEncoding());
            if (body.length() > 0) {
                msg.append("\n<< Body: ").append(body);
            }
        }

        msg.append("\n<<<<<<<<<<<<<<<<<<<<<<");

        return msg.toString();
    }

    public static class PreReadRequestWrapper extends HttpServletRequestWrapper {
        private final String body;

        public PreReadRequestWrapper(HttpServletRequest request) throws IOException{
            super(request);

            StringBuilder stringBuilder = new StringBuilder(2000);
            BufferedReader bufferedReader = null;
            try {
                InputStream inputStream = request.getInputStream();
                if (inputStream != null) {
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    char[] charBuffer = new char[128];
                    int bytesRead = -1;
                    while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                        stringBuilder.append(charBuffer, 0, bytesRead);
                    }
                }
            } catch (IOException ex) {
                throw ex;
            } finally {
                IOUtils.closeQuietly(bufferedReader);
            }
            body = stringBuilder.toString();
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
            ServletInputStream servletInputStream = new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {

                }

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
            };
            return servletInputStream;
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(this.getInputStream()));
        }

        public String getBody() {
            return this.body;
        }
    }
}
