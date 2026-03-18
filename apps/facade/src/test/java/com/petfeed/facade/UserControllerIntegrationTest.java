package com.petfeed.facade;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.*;

import javax.annotation.Nonnull;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Facade 통합테스트
 * <p>
 * 실제 서버를 띄우지 않고 WireMock이 user-service / post-service를 흉내냅니다.
 * <p>
 *   [Test] → Facade (실제 Spring Boot) → WireMock (가짜 user-service & post-service)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)  // 랜덤 포트로 WireMock 기동, ${wiremock.server.port}로 주입됨
class UserControllerIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;  // Facade 서버로 실제 HTTP 요청을 보내는 클라이언트

    @Test
    void signup_success() {
        String body = """
                {
                  "userKey": "test-user-key",
                  "createdAt": "2026-03-18T00:00:00"
                }
                """;
        stubFor(post(urlEqualTo("/api/v1/users/signup"))
                .willReturn(getResult(body)));

        String requestBody = """
                {
                  "email": "test@petfeed.com",
                  "password": "password123",
                  "phone": "010-1234-5678",
                  "nickname": "petlover"
                }
                """;

        HttpHeaders headers = getHttpHeaders();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/users/signup",
                new HttpEntity<>(requestBody, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("test-user-key");

        // WireMock에 실제로 요청이 들어왔는지 검증
        verify(postRequestedFor(urlEqualTo("/api/v1/users/signup")));
    }

    @Test
    void getProfile_success() {
        String userKey = "test-user-key";

        String body = """
                {
                  "nickname": "petlover",
                  "profileImageUrl": null,
                  "bio": "강아지 좋아",
                  "followerCount": 10,
                  "followingCount": 5,
                  "following": false,
                  "postCount": 0
                }
                """;
        stubFor(get(urlEqualTo("/api/v1/users/" + userKey))
                .willReturn(getResult(body)));

        // post-service stub: getPostCount
        stubFor(get(urlEqualTo("/api/v1/posts/count/" + userKey))
                .willReturn(getResult("3")));

        HttpHeaders headers = getHttpHeaders();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + userKey,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("petlover");
    }

    @Nonnull
    private static HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Key", "requester-key");  // 인증 헤더 (Gateway가 주입하는 헤더)
        return headers;
    }

    /**
     * [E2E] signup → getProfile
     * signup으로 받은 userKey로 프로필 조회까지 이어지는 흐름 검증
     */
    @Test
    void after_signup_then_getProfile_E2E() {

        String body = """
                {"userKey": "e2e-user-key", "createdAt": "2026-03-18T00:00:00"}
                """;
        stubFor(post(urlEqualTo("/api/v1/users/signup"))
                .willReturn(getResult(body)));

        String profileMock = """
                                {
                                  "nickname": "e2eUser",
                                  "profileImageUrl": null,
                                  "bio": null,
                                  "followerCount": 0,
                                  "followingCount": 0,
                                  "following": false,
                                  "postCount": 0
                                }
                                """;
        stubFor(get(urlEqualTo("/api/v1/users/e2e-user-key"))
                .willReturn(getResult(profileMock)));


        stubFor(get(urlEqualTo("/api/v1/posts/count/e2e-user-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("0")));

        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> signupResponse = restTemplate.postForEntity(
                "/api/v1/users/signup",
                new HttpEntity<>("""
                        {"email":"e2e@test.com","password":"pass","phone":"010-0000-0000","nickname":"e2eUser"}
                        """, jsonHeaders),
                String.class
        );
        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpHeaders profileHeaders = new HttpHeaders();
        profileHeaders.set("X-User-Key", "e2e-user-key");

        ResponseEntity<String> profileResponse = restTemplate.exchange(
                "/api/v1/users/e2e-user-key",
                HttpMethod.GET,
                new HttpEntity<>(profileHeaders),
                String.class
        );
        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profileResponse.getBody()).contains("e2eUser");
    }

    public static ResponseDefinitionBuilder getResult(String body) {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }
}
