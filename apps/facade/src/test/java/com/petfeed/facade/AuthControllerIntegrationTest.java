package com.petfeed.facade;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class AuthControllerIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void login_success() {
        stubFor(post(urlEqualTo("/api/v1/auth/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "accessToken": "mocked-access-token",
                                  "refreshToken": "mocked-refresh-token",
                                  "accessTokenExpiresIn": "3600000",
                                  "grantType": "Bearer"
                                }
                                """)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>("""
                        {
                          "email": "test@petfeed.com",
                          "password": "password123!"
                        }
                        """, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("mocked-access-token");
        assertThat(response.getBody()).contains("Bearer");
        verify(postRequestedFor(urlEqualTo("/api/v1/auth/login")));
    }
}
