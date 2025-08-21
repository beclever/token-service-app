package com.vincent.learning.token.access;

import static com.vincent.learning.token.util.Utils.toJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.vincent.learning.token.config.IamProperties;
import com.vincent.learning.token.exception.ErrorConstants;
import com.vincent.learning.token.exception.RestException;
import com.vincent.learning.token.model.ErrorResponse;
import com.vincent.learning.token.model.GrantType;
import com.vincent.learning.token.model.IamToken;
import com.vincent.learning.token.model.TokenResponse;
import com.vincent.learning.token.service.WebClientFactory;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = TokenController.class)
class TokenControllerTest {
    @Autowired private WebTestClient webClient;

    @MockBean private IamProperties iamProperties;

    @MockBean private WebClientFactory webClientFactory;
    @Mock private WebClient iamWebClient;

    @Mock private WebClient.RequestBodyUriSpec requestBodyUriMock;
    @Mock private WebClient.ResponseSpec responseMock;
    @Mock private WebClient.RequestBodySpec requestBodyMock;
    @Mock private WebClient.RequestHeadersSpec requestHeadersMock;

    @BeforeEach
    public void init() {
        when(iamProperties.getClientId()).thenReturn("clientId");
        when(webClientFactory.getWebClient()).thenReturn(iamWebClient);
    }

    @Test
    void testPasswordGrant() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        String username = "user1";
        String password = "passwd";
        formData.add("username", username);
        formData.add("password", encode(password));
        formData.add("grant_type", GrantType.PASSWORD.name());

        TokenResponse tokenResponse =
                TokenResponse.builder()
                        .accessToken("accessToken")
                        .tokenType("Bearer")
                        .expiresIn(10)
                        .refreshToken("refreshToken")
                        .build();

        mockNormalResponse();

        assertEquals(
                toJson(tokenResponse),
                new String(
                        webClient
                                .post()
                                .uri("/iam/openid-connect/v1/token")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .body(BodyInserters.fromFormData(formData))
                                .exchange()
                                .expectStatus()
                                .is2xxSuccessful()
                                .expectBody()
                                .returnResult()
                                .getResponseBody()));

        ArgumentCaptor<BodyInserters.FormInserter<String>> bodyAc =
                ArgumentCaptor.forClass(BodyInserters.FormInserter.class);
        verify(requestBodyMock, times(1)).body(bodyAc.capture());
        Object requestBody = ReflectionTestUtils.getField(bodyAc.getValue(), "data");
        if (requestBody instanceof LinkedMultiValueMap) {
            assertEquals(
                    "password",
                    ((LinkedMultiValueMap<String, String>) requestBody).get("grant_type").get(0));
            assertEquals(
                    username,
                    ((LinkedMultiValueMap<String, String>) requestBody).get("username").get(0));
            assertEquals(
                    password,
                    ((LinkedMultiValueMap<String, String>) requestBody).get("password").get(0));
            assertEquals(
                    iamProperties.getClientId(),
                    ((LinkedMultiValueMap<String, String>) requestBody).get("client_id").get(0));
        }
    }

    private static String encode(String password) {
        return Base64.getEncoder().encodeToString(password.getBytes());
    }

    @Test
    void testRefreshTokenGrant() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        String refreshToken = "refresh1";
        formData.add("refresh_token", refreshToken);
        formData.add("grant_type", GrantType.REFRESH_TOKEN.name());

        TokenResponse tokenResponse =
                TokenResponse.builder()
                        .accessToken("accessToken")
                        .tokenType("Bearer")
                        .expiresIn(10)
                        .refreshToken("refreshToken")
                        .build();

        mockNormalResponse();

        assertEquals(
                toJson(tokenResponse),
                new String(
                        webClient
                                .post()
                                .uri("/iam/openid-connect/v1/token")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .body(BodyInserters.fromFormData(formData))
                                .exchange()
                                .expectStatus()
                                .is2xxSuccessful()
                                .expectBody()
                                .returnResult()
                                .getResponseBody()));

        ArgumentCaptor<BodyInserters.FormInserter<String>> bodyAc =
                ArgumentCaptor.forClass(BodyInserters.FormInserter.class);
        verify(requestBodyMock, times(1)).body(bodyAc.capture());
        Object requestBody = ReflectionTestUtils.getField(bodyAc.getValue(), "data");
        if (requestBody instanceof LinkedMultiValueMap) {
            assertEquals(
                    "refresh_token",
                    ((LinkedMultiValueMap<String, String>) requestBody).get("grant_type").get(0));
            assertEquals(
                    refreshToken,
                    ((LinkedMultiValueMap<String, String>) requestBody)
                            .get("refresh_token")
                            .get(0));
            assertEquals(
                    iamProperties.getClientId(),
                    ((LinkedMultiValueMap<String, String>) requestBody).get("client_id").get(0));
        }
    }

    @Test
    void testIamReturnError() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        String username = "user1";
        String password = "passwd";
        formData.add("username", username);
        formData.add("password", encode(password));
        formData.add("grant_type", GrantType.PASSWORD.name());

        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .error(ErrorConstants.INVALID_GRANT_TYPE)
                        .message("invalid")
                        .build();

        mockErrorResponse();

        assertEquals(
                toJson(errorResponse),
                new String(
                        webClient
                                .post()
                                .uri("/iam/openid-connect/v1/token")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .body(BodyInserters.fromFormData(formData))
                                .exchange()
                                .expectStatus()
                                .isUnauthorized()
                                .expectBody()
                                .returnResult()
                                .getResponseBody()));
    }

    private void mockNormalResponse() {
        IamToken iamToken =
                IamToken.builder()
                        .accessToken("accessToken")
                        .refreshToken("refreshToken")
                        .expiresIn(10)
                        .tokenType("Bearer")
                        .build();

        when(iamWebClient.post()).thenReturn(requestBodyUriMock);
        when(requestBodyUriMock.uri("/token")).thenReturn(requestBodyMock);
        when(requestBodyMock.body(any())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header(any(), any())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.onStatus(any(), any())).thenReturn(responseMock);
        when(responseMock.bodyToMono(IamToken.class)).thenReturn(Mono.just(iamToken));
    }

    private void mockErrorResponse() {

        CustomMinimalForTestResponseSpec responseSpecMock =
                mock(CustomMinimalForTestResponseSpec.class);

        when(iamWebClient.post()).thenReturn(requestBodyUriMock);
        when(requestBodyUriMock.uri("/token")).thenReturn(requestBodyMock);
        when(requestBodyMock.body(any())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header(any(), any())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);
        when(responseSpecMock.onStatus(any(Predicate.class), any(Function.class)))
                .thenCallRealMethod();
    }

    @Test
    void testMissingParameter() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", GrantType.REFRESH_TOKEN.name());

        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .error(ErrorConstants.MISSING_MANDATORY)
                        .message("refresh_token is missing")
                        .build();

        verifyBadRequest(formData, errorResponse);

        formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", GrantType.PASSWORD.name());

        errorResponse =
                ErrorResponse.builder()
                        .error(ErrorConstants.MISSING_MANDATORY)
                        .message("username is missing")
                        .build();

        verifyBadRequest(formData, errorResponse);

        formData.add("username", "username");
        errorResponse =
                ErrorResponse.builder()
                        .error(ErrorConstants.MISSING_MANDATORY)
                        .message("password is missing")
                        .build();

        verifyBadRequest(formData, errorResponse);

        formData = new LinkedMultiValueMap<>();
        formData.add("username", "user");
        errorResponse =
                ErrorResponse.builder()
                        .error(ErrorConstants.INVALID_GRANT_TYPE)
                        .message("grant type is invalid")
                        .build();

        verifyBadRequest(formData, errorResponse);
    }

    @Test
    void testInvalidFormatOfPassword() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        String username = "user1";
        String password = "cGF43dkaaa--==";
        formData.add("username", username);
        formData.add("password", password);
        formData.add("grant_type", GrantType.PASSWORD.name());

        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .error(ErrorConstants.PASSWORD_INVALID_FORMAT)
                        .message("fail to decode password with base64")
                        .build();

        verifyBadRequest(formData, errorResponse);
    }

    private void verifyBadRequest(
            MultiValueMap<String, String> formData, ErrorResponse errorResponse) {
        assertEquals(
                toJson(errorResponse),
                new String(
                        webClient
                                .post()
                                .uri("/iam/openid-connect/v1/token")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .body(BodyInserters.fromFormData(formData))
                                .exchange()
                                .expectStatus()
                                .isBadRequest()
                                .expectBody()
                                .returnResult()
                                .getResponseBody()));
    }

    abstract class CustomMinimalForTestResponseSpec implements WebClient.ResponseSpec {

        public abstract HttpStatus getStatus();

        public WebClient.ResponseSpec onStatus(
                Predicate<HttpStatusCode> statusPredicate,
                Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {
            if (statusPredicate.test(this.getStatus())) {
                ErrorResponse errorResponse =
                        ErrorResponse.builder()
                                .error(ErrorConstants.INVALID_GRANT_TYPE)
                                .message("invalid")
                                .build();
                try {
                    exceptionFunction
                            .apply(
                                    ClientResponse.create(HttpStatus.UNAUTHORIZED)
                                            .body(toJson(errorResponse))
                                            .header(
                                                    HttpHeaders.CONTENT_TYPE,
                                                    MediaType.APPLICATION_JSON_VALUE)
                                            .build())
                            .toFuture()
                            .get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof RestException) {
                        throw new RestException(
                                ((RestException) e.getCause()).getError(),
                                ((RestException) e.getCause()).getErrorMessage(),
                                ((RestException) e.getCause()).getStatus());
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }
            return this;
        }

        public void dummy() {
            // do nothing
        }
    }
}
