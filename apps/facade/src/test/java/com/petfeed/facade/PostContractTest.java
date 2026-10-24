package com.petfeed.facade;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
class PostContractTest {
    @Autowired MockMvc mvc;

    @Test void createForwardsIdentityAndBodyAndComposesAuthor() throws Exception {
        stubFor(get(urlEqualTo("/internal/v1/users/post-owner/author"))
                .willReturn(okJson("{\"status\":\"ACTIVE\",\"displayName\":\"owner\"}")));
        stubFor(post(urlEqualTo("/api/v1/posts"))
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"postId\":1,\"userKey\":\"post-owner\",\"content\":\"hello\"}")));
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/posts").header("X-User-Key", "post-owner")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hello\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.postId").value(1))
                .andExpect(jsonPath("$.author.displayName").value("owner"));
        verify(postRequestedFor(urlEqualTo("/api/v1/posts"))
                .withHeader("X-User-Key", equalTo("post-owner"))
                .withRequestBody(matchingJsonPath("$.content", equalTo("hello"))));
    }

    @Test void detailPreservesWithdrawnAuthorAndLookupFailureStatus() throws Exception {
        stubFor(get(urlEqualTo("/api/v1/posts/91"))
                .willReturn(okJson("{\"postId\":91,\"userKey\":\"withdrawn-post-author\",\"content\":\"retained\"}")));
        stubFor(get(urlEqualTo("/internal/v1/users/withdrawn-post-author/author"))
                .willReturn(okJson("{\"status\":\"WITHDRAWN\",\"displayName\":\"(탈퇴한 회원)\"}")));
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/91").header("X-User-Key", "viewer"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("retained"))
                .andExpect(jsonPath("$.author.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.author.displayName").value("(탈퇴한 회원)"));
        stubFor(get(urlEqualTo("/internal/v1/users/withdrawn-post-author/author"))
                .willReturn(aResponse().withStatus(503)));
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/91").header("X-User-Key", "viewer"))
                .andExpect(status().isServiceUnavailable());
    }
}
