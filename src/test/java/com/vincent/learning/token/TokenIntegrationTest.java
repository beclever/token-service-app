package com.vincent.learning.token;

import static com.vincent.learning.token.util.Utils.toJson;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vincent.learning.token.exception.ErrorConstants;
import com.vincent.learning.token.model.ErrorResponse;
import com.vincent.learning.token.model.GrantType;
import com.vincent.learning.token.model.IamToken;
import com.vincent.learning.token.model.TokenResponse;
import com.vincent.learning.token.util.EncodeUtil;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@AutoConfigureWebTestClient(timeout = "10000")
@ActiveProfiles("test")
class TokenIntegrationTest {

    private String baseUrl;
    private WireMockServer wireMockServer;

    @Autowired private WebTestClient webClient;

    @BeforeEach
    public void setUp() {
        wireMockServer = new WireMockServer(31000);
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        baseUrl =
                format(
                        "http://localhost:%s/auth/realms/oam/protocol/openid-connect",
                        wireMockServer.port());
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void test_get_token() {
        IamToken iamToken =
                IamToken.builder()
                        .accessToken("accessToken")
                        .refreshToken("refreshToken")
                        .expiresIn(10)
                        .tokenType("Bearer")
                        .build();
        String requestBody =
                "grant_type=password&username=user1&password=passwd&client_id=admin-portal";
        stubPostResponse(
                "/auth/realms/oam/protocol/openid-connect/token",
                200,
                requestBody,
                toJson(iamToken));

        TokenResponse tokenResponse =
                TokenResponse.builder()
                        .accessToken("accessToken")
                        .tokenType("Bearer")
                        .expiresIn(10)
                        .refreshToken("refreshToken")
                        .build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        String username = "user1";
        String password = "passwd";
        formData.add("username", username);
        formData.add("password", EncodeUtil.encodeBase64(password));
        formData.add("grant_type", GrantType.PASSWORD.name());

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
                                .isOk()
                                .expectBody()
                                .returnResult()
                                .getResponseBody()));
    }

    @Test
    void test_refresh_token() {
        IamToken iamToken =
                IamToken.builder()
                        .accessToken("accessToken")
                        .refreshToken("refreshToken")
                        .expiresIn(10)
                        .tokenType("Bearer")
                        .build();
        String requestBody = "grant_type=refresh_token&refresh_token=token1&client_id=admin-portal";
        stubPostResponse(
                "/auth/realms/oam/protocol/openid-connect/token",
                200,
                requestBody,
                toJson(iamToken));

        TokenResponse tokenResponse =
                TokenResponse.builder()
                        .accessToken("accessToken")
                        .tokenType("Bearer")
                        .expiresIn(10)
                        .refreshToken("refreshToken")
                        .build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("refresh_token", "token1");
        formData.add("grant_type", GrantType.REFRESH_TOKEN.name());

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
    }

    @Test
    void test_iam_error() {
        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .error(ErrorConstants.INVALID_GRANT_TYPE)
                        .message("invalid")
                        .build();
        String requestBody =
                "grant_type=refresh_token&refresh_token=test_iam_error&client_id=admin-portal";
        stubPostResponse(
                "/auth/realms/oam/protocol/openid-connect/token",
                400,
                requestBody,
                toJson(errorResponse));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("refresh_token", "test_iam_error");
        formData.add("grant_type", GrantType.REFRESH_TOKEN.name());

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

    @Test
    void test_validate_error() {
        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .error(ErrorConstants.INVALID_GRANT_TYPE)
                        .message("grant type is invalid")
                        .build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

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

    @Test
    void test_iam_path_not_found() {
        ErrorResponse errorResponse =
                ErrorResponse.builder().error(ErrorConstants.INTERNAL_ERROR).build();
        stubPostResponse(
                "/auth/realms/oam/protocol/openid-connect/token", 400, "", toJson(errorResponse));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("refresh_token", "test_iam_error");
        formData.add("grant_type", GrantType.REFRESH_TOKEN.name());

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
                                .is5xxServerError()
                                .expectBody()
                                .returnResult()
                                .getResponseBody()));
    }

    private static void stubPostResponse(
            String url, int statusCode, String requestBody, String response) {
        stubFor(
                post(urlEqualTo(url))
                        .withHeader(
                                HttpHeaders.CONTENT_TYPE,
                                containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .withRequestBody(equalTo(requestBody))
                        .willReturn(
                                aResponse()
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withStatus(statusCode)
                                        .withBody(response)));
    }
}
