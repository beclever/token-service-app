package com.vincent.learning.token;

import static com.vincent.learning.token.util.Utils.toJson;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vincent.learning.token.model.GrantType;
import com.vincent.learning.token.model.IamToken;
import com.vincent.learning.token.model.TokenResponse;
import com.vincent.learning.token.util.EncodeUtil;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
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
@ActiveProfiles("httpsTest")
class TokenIntegrationHttpsTest {

    private WireMockServer wireMockServer;

    @Autowired private WebTestClient webClient;

    @Value("classpath:identity.jks")
    Resource resourceFile;

    @BeforeEach
    public void setUp() throws IOException {
        wireMockServer =
                new WireMockServer(
                        wireMockConfig()
                                .port(53000)
                                .httpsPort(8944)
                                .keystorePath(resourceFile.getFile().getPath()));
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
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
                                .is2xxSuccessful()
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
