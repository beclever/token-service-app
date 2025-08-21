package com.vincent.learning.token.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@Setter
public class RestException extends RuntimeException {
    private final String error;
    private final String errorMessage;
    private final HttpStatusCode status;

    public RestException(String error, String errorMessage, HttpStatusCode status) {
        super(error);
        this.error = error;
        this.errorMessage = errorMessage;
        this.status = status;
    }

    public RestException(String error, String errorMessage) {
        this(error, errorMessage, HttpStatus.BAD_REQUEST);
    }
}
