package com.vincent.learning.token.access;

import static com.vincent.learning.token.util.EncodeUtil.decodeBase64;

import com.vincent.learning.token.config.IamProperties;
import com.vincent.learning.token.exception.ErrorConstants;
import com.vincent.learning.token.exception.RestException;
import com.vincent.learning.token.model.*;
import com.vincent.learning.token.model.*;
import com.vincent.learning.token.service.WebClientFactory;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/iam/openid-connect/v1")
@Slf4j
public class TokenController {

    @Autowired private WebClientFactory webClientFactory;

    @Autowired private IamProperties iamProperties;

    @PostMapping(path = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<TokenResponse> exchangeToken(ExchangeTokenRequest tokenRequest) {

        if (log.isDebugEnabled()) {
            log.debug("receive the request {}", tokenRequest);
        }

        ErrorResponse validateError = validate(tokenRequest);
        if (Objects.nonNull(validateError)) {
            return Mono.error(
                    new RestException(validateError.getError(), validateError.getMessage()));
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        String grantType = tokenRequest.getGrantType().toLowerCase();
        if (GrantType.PASSWORD.name().toLowerCase().equals(grantType)) {
            formData.add("grant_type", grantType);
            formData.add("username", tokenRequest.getUsername());
            try {
                formData.add("password", decodeBase64(tokenRequest.getPassword()));
            } catch (Exception e) {
                return Mono.error(
                        new RestException(
                                ErrorConstants.PASSWORD_INVALID_FORMAT,
                                "fail to decode password with base64"));
            }

            formData.add("client_id", iamProperties.getClientId());
        } else {
            formData.add("grant_type", grantType);
            formData.add("refresh_token", tokenRequest.getRefreshToken());
            formData.add("client_id", iamProperties.getClientId());
        }

        if (log.isDebugEnabled()) {
            log.debug("the request form data {}", formData);
        }

        return webClientFactory
                .getWebClient()
                .post()
                .uri("/token")
                .body(BodyInserters.fromFormData(formData))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        errorResponse ->
                                errorResponse
                                        .bodyToMono(ErrorResponse.class)
                                        .flatMap(
                                                error -> {
                                                    if (log.isDebugEnabled()) {
                                                        log.debug(
                                                                "error {}, response {}",
                                                                error,
                                                                errorResponse.statusCode());
                                                    }
                                                    return Mono.error(
                                                            new RestException(
                                                                    error.getError(),
                                                                    error.getMessage(),
                                                                    errorResponse.statusCode()));
                                                }))
                .bodyToMono(IamToken.class)
                .flatMap(
                        iamToken ->
                                Mono.just(
                                        new TokenResponse(
                                                iamToken.getAccessToken(),
                                                iamToken.getTokenType(),
                                                iamToken.getExpiresIn(),
                                                iamToken.getRefreshToken())));
    }

    private ErrorResponse validate(ExchangeTokenRequest tokenRequest) {
        if (GrantType.PASSWORD.name().equalsIgnoreCase(tokenRequest.getGrantType())) {
            if (!StringUtils.hasText(tokenRequest.getUsername())) {
                return new ErrorResponse(ErrorConstants.MISSING_MANDATORY, "username is missing");
            }
            if (!StringUtils.hasText(tokenRequest.getPassword())) {
                return new ErrorResponse(ErrorConstants.MISSING_MANDATORY, "password is missing");
            }
        } else if (GrantType.REFRESH_TOKEN.name().equalsIgnoreCase(tokenRequest.getGrantType())) {
            if (!StringUtils.hasText(tokenRequest.getRefreshToken())) {
                return new ErrorResponse(
                        ErrorConstants.MISSING_MANDATORY, "refresh_token is missing");
            }
        } else {
            return new ErrorResponse(ErrorConstants.INVALID_GRANT_TYPE, "grant type is invalid");
        }

        return null;
    }
}
