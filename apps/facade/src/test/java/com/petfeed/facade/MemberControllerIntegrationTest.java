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
class MemberControllerIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @org.junit.jupiter.api.BeforeEach
    void followDefaults() {
        stubFor(get(urlMatching("/internal/v1/members/[^/]+/author")).atPriority(10)
                .willReturn(okJson("{\"status\":\"ACTIVE\",\"displayName\":\"member\"}")));
        stubFor(get(urlPathMatching("/internal/v1/members/[^/]+/(followers|followings)")).atPriority(10)
                .willReturn(okJson("{\"entries\":[],\"nextCursor\":null,\"hasNext\":false}")));
        stubFor(get(urlPathEqualTo("/internal/v1/follows/targets")).willReturn(okJson("[]")));
    }
    private static final String MEMBER_KEY = "test-member-key";

    @Test
    void withdrawnAuthorIsSerializedInPostList() {
        stubFor(get(urlPathEqualTo("/internal/v1/comments/counts")).willReturn(okJson("{}")));
        String key = "withdrawn-owner";
        stubFor(get(urlEqualTo("/internal/v1/members/" + key + "/author"))
                .willReturn(okJson("{\"status\":\"WITHDRAWN\",\"displayName\":\"(탈퇴한 회원)\"}")));
        stubFor(get(urlPathEqualTo("/api/v1/members/" + key + "/posts"))
                .willReturn(okJson("{\"posts\":[{\"postId\":42}],\"nextCursor\":null,\"hasNext\":false}")));

        var response = restTemplate.exchange("/api/v1/members/" + key + "/posts", HttpMethod.GET,
                new HttpEntity<>(memberKeyHeader()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"postId\":42", "\"status\":\"WITHDRAWN\"", "(탈퇴한 회원)");
    }

    @Test
    void authorServiceFailureDoesNotReturnWithdrawnAuthor() {
        String key = "unavailable-owner";
        stubFor(get(urlEqualTo("/internal/v1/members/" + key + "/author"))
                .willReturn(aResponse().withStatus(503)));

        var response = restTemplate.exchange("/api/v1/members/" + key + "/posts", HttpMethod.GET,
                new HttpEntity<>(memberKeyHeader()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNullOrEmpty();
    }

    @Test
    void signup_success() {
        stubFor(post(urlEqualTo("/api/v1/members/signup"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "memberKey": "%s",
                                  "createdAt": "2026-03-19T00:00:00"
                                }
                                """.formatted(MEMBER_KEY))));

        HttpHeaders headers = jsonHeaders();
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/members/signup",
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains(MEMBER_KEY);
        verify(postRequestedFor(urlEqualTo("/api/v1/members/signup")));
    }

    @Test
    void getProfile_success() {
        // member-service: 프로필 조회
        stubFor(get(urlEqualTo("/api/v1/members/" + MEMBER_KEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "nickname": "petlover",
                                  "profileImageUrl": null,
                                  "bio": "member bio"
                                }
                                """)));

        // post-service: postCount 조회 (MemberFacade에서 조합)
        stubFor(get(urlEqualTo("/api/v1/posts/count/" + MEMBER_KEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("3")));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/members/" + MEMBER_KEY,
                HttpMethod.GET,
                new HttpEntity<>(memberKeyHeader()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("petlover");
        assertThat(response.getBody()).contains("\"postCount\":3");
        verify(getRequestedFor(urlEqualTo("/api/v1/members/" + MEMBER_KEY)));
        verify(getRequestedFor(urlEqualTo("/api/v1/posts/count/" + MEMBER_KEY)));
    }

    @Test
    void getPostsByMember_success() {
        stubFor(get(urlPathEqualTo("/internal/v1/comments/counts")).willReturn(okJson("{\"1\":2}")));
        stubFor(get(urlEqualTo("/internal/v1/members/" + MEMBER_KEY + "/author"))
                .willReturn(okJson("{\"status\":\"ACTIVE\",\"displayName\":\"petlover\"}")));
        stubFor(get(urlPathEqualTo("/api/v1/members/" + MEMBER_KEY + "/posts"))
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
                "/api/v1/members/" + MEMBER_KEY + "/posts",
                HttpMethod.GET,
                new HttpEntity<>(memberKeyHeader()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("thumbnailUrl");
        assertThat(response.getBody()).contains("\"status\":\"ACTIVE\"", "\"displayName\":\"petlover\"");
        verify(getRequestedFor(urlPathEqualTo("/api/v1/members/" + MEMBER_KEY + "/posts")));
    }

    @Test
    void getFollowers_success() {
        stubFor(get(urlPathEqualTo("/internal/v1/members/" + MEMBER_KEY + "/followers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "entries": [
                                    {
                                      "followId": 1, "memberKey": "follower-key",
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
                "/api/v1/members/" + MEMBER_KEY + "/followers",
                HttpMethod.GET,
                new HttpEntity<>(memberKeyHeader()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("follower-key");
        verify(getRequestedFor(urlPathEqualTo("/internal/v1/members/" + MEMBER_KEY + "/followers")));
    }

    @Test
    void getFollowings_success() {
        stubFor(get(urlPathEqualTo("/internal/v1/members/" + MEMBER_KEY + "/followings"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "entries": [
                                    {
                                      "followId": 2, "memberKey": "following-key",
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
                "/api/v1/members/" + MEMBER_KEY + "/followings",
                HttpMethod.GET,
                new HttpEntity<>(memberKeyHeader()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("following-key");
        verify(getRequestedFor(urlPathEqualTo("/internal/v1/members/" + MEMBER_KEY + "/followings")));
    }

    @Test
    void updateProfile_success() {
        stubFor(put(urlEqualTo("/api/v1/members/" + MEMBER_KEY))
                .willReturn(aResponse()
                        .withStatus(204)));

        HttpHeaders headers = jsonHeaders();
        headers.set("X-Member-Key", MEMBER_KEY);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/members/" + MEMBER_KEY,
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
        verify(putRequestedFor(urlEqualTo("/api/v1/members/" + MEMBER_KEY)));
    }

    @Test
    void follow_success() {
        stubFor(post(urlEqualTo("/api/v1/following/" + MEMBER_KEY))
                .willReturn(aResponse()
                        .withStatus(200)));

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/following/" + MEMBER_KEY,
                HttpMethod.POST,
                new HttpEntity<>(memberKeyHeader()),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(postRequestedFor(urlEqualTo("/api/v1/following/" + MEMBER_KEY)).withHeader("X-Member-Key", equalTo("requester-key")));
    }

    @Test
    void unfollow_success() {
        stubFor(delete(urlEqualTo("/api/v1/following/" + MEMBER_KEY))
                .willReturn(aResponse()
                        .withStatus(200)));

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/following/" + MEMBER_KEY,
                HttpMethod.DELETE,
                new HttpEntity<>(memberKeyHeader()),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deleteRequestedFor(urlEqualTo("/api/v1/following/" + MEMBER_KEY)).withHeader("X-Member-Key", equalTo("requester-key")));
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders memberKeyHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Member-Key", "requester-key");
        return headers;
    }
}
