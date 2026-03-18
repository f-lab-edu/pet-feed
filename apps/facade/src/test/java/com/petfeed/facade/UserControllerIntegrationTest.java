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
class UserControllerIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    private static final String USER_KEY = "test-user-key";

    @Test
    void signup_success() {
        stubFor(post(urlEqualTo("/api/v1/users/signup"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "userKey": "%s",
                                  "createdAt": "2026-03-19T00:00:00"
                                }
                                """.formatted(USER_KEY))));

        HttpHeaders headers = jsonHeaders();
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/users/signup",
                new HttpEntity<>("""
                        {
                          "email": "test@petfeed.com",
                          "password": "password123!",
                          "phone": "010-1234-5678",
                          "nickname": "petlover"
                        }
                        """, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(USER_KEY);
        verify(postRequestedFor(urlEqualTo("/api/v1/users/signup")));
    }

    @Test
    void getProfile_success() {
        // user-service: 프로필 조회
        stubFor(get(urlEqualTo("/api/v1/users/" + USER_KEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "nickname": "petlover",
                                  "profileImageUrl": null,
                                  "bio": "강아지 좋아",
                                  "followerCount": 10,
                                  "followingCount": 5,
                                  "following": false
                                }
                                """)));

        // post-service: postCount 조회 (UserFacade에서 조합)
        stubFor(get(urlEqualTo("/api/v1/posts/count/" + USER_KEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("3")));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + USER_KEY,
                HttpMethod.GET,
                new HttpEntity<>(userKeyHeader()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("petlover");
        assertThat(response.getBody()).contains("\"postCount\":3");
        verify(getRequestedFor(urlEqualTo("/api/v1/users/" + USER_KEY)));
        verify(getRequestedFor(urlEqualTo("/api/v1/posts/count/" + USER_KEY)));
    }

    @Test
    void getPostsByUser_success() {
        stubFor(get(urlPathEqualTo("/api/v1/users/" + USER_KEY + "/posts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "posts": [
                                    {
                                      "postId": 1,
                                      "thumbnailUrl": "https://example.com/thumb.jpg",
                                      "likeCount": 5,
                                      "commentCount": 2,
                                      "createdAt": "2026-03-19T00:00:00",
                                      "updatedAt": "2026-03-19T00:00:00"
                                    }
                                  ],
                                  "nextCursor": null,
                                  "hasNext": false
                                }
                                """)));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + USER_KEY + "/posts",
                HttpMethod.GET,
                new HttpEntity<>(userKeyHeader()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("thumbnailUrl");
        verify(getRequestedFor(urlPathEqualTo("/api/v1/users/" + USER_KEY + "/posts")));
    }

    @Test
    void getFollowers_success() {
        stubFor(get(urlPathEqualTo("/api/v1/users/" + USER_KEY + "/followers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "users": [
                                    {
                                      "userKey": "follower-key",
                                      "nickname": "follower1",
                                      "profileImageUrl": null,
                                      "following": false
                                    }
                                  ],
                                  "nextCursor": null,
                                  "hasNext": false
                                }
                                """)));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + USER_KEY + "/followers",
                HttpMethod.GET,
                new HttpEntity<>(userKeyHeader()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("follower-key");
        verify(getRequestedFor(urlPathEqualTo("/api/v1/users/" + USER_KEY + "/followers")));
    }

    @Test
    void getFollowings_success() {
        stubFor(get(urlPathEqualTo("/api/v1/users/" + USER_KEY + "/followings"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "users": [
                                    {
                                      "userKey": "following-key",
                                      "nickname": "following1",
                                      "profileImageUrl": null,
                                      "following": true
                                    }
                                  ],
                                  "nextCursor": null,
                                  "hasNext": false
                                }
                                """)));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + USER_KEY + "/followings",
                HttpMethod.GET,
                new HttpEntity<>(userKeyHeader()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("following-key");
        verify(getRequestedFor(urlPathEqualTo("/api/v1/users/" + USER_KEY + "/followings")));
    }

    @Test
    void updateProfile_success() {
        stubFor(put(urlEqualTo("/api/v1/users/" + USER_KEY))
                .willReturn(aResponse()
                        .withStatus(204)));

        HttpHeaders headers = jsonHeaders();
        headers.set("X-User-Key", USER_KEY);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/users/" + USER_KEY,
                HttpMethod.PUT,
                new HttpEntity<>("""
                        {
                          "bio": "업데이트된 소개",
                          "email": "updated@petfeed.com"
                        }
                        """, headers),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(putRequestedFor(urlEqualTo("/api/v1/users/" + USER_KEY)));
    }

    @Test
    void follow_success() {
        stubFor(post(urlEqualTo("/api/v1/following/" + USER_KEY))
                .willReturn(aResponse()
                        .withStatus(200)));

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/following/" + USER_KEY,
                HttpMethod.POST,
                new HttpEntity<>(userKeyHeader()),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(postRequestedFor(urlEqualTo("/api/v1/following/" + USER_KEY)));
    }

    @Test
    void unfollow_success() {
        stubFor(delete(urlEqualTo("/api/v1/following/" + USER_KEY))
                .willReturn(aResponse()
                        .withStatus(200)));

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/following/" + USER_KEY,
                HttpMethod.DELETE,
                new HttpEntity<>(userKeyHeader()),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deleteRequestedFor(urlEqualTo("/api/v1/following/" + USER_KEY)));
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders userKeyHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Key", "requester-key");
        return headers;
    }
}
