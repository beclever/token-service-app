package com.vincent.learning.token.exception;

import static com.vincent.learning.token.exception.ErrorConstants.INTERNAL_ERROR;

import com.vincent.learning.token.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.vincent.learning.token")
public class GlobalExceptionHandler {

    public static final MultiValueMap<String, String> PROBLEM_DETAIL_HEADER;

    static {
        final MultiValueMap<String, String> temp = new LinkedMultiValueMap<>();
        temp.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        PROBLEM_DETAIL_HEADER = CollectionUtils.unmodifiableMultiValueMap(temp);
    }

    @ResponseBody
    @ExceptionHandler(value = RestException.class)
    public ResponseEntity<ErrorResponse> restExceptionHandler(RestException e) {
        log.debug(
                "rest exception info is {}, {}, {}",
                e.getError(),
                e.getErrorMessage(),
                e.getStackTrace());
        HttpStatusCode httpStatus = e.getStatus();
        ErrorResponse error = new ErrorResponse(e.getError(), e.getErrorMessage());
        return new ResponseEntity<>(error, PROBLEM_DETAIL_HEADER, httpStatus);
    }

    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> internalExceptionHandler(Exception e) {
        log.error("The exception detail:", e);
        ErrorResponse error = new ErrorResponse(INTERNAL_ERROR, null);
        return new ResponseEntity<>(error, PROBLEM_DETAIL_HEADER, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
