package com.petfeed.facade;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.web.servlet.MockMvc;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
class CommentContractTest {
    @Autowired MockMvc mvc;

    @Test void createChecksPostAndForwardsIdentityToCommentApi() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/internal/v1/users/reader/author"))
                .willReturn(okJson("{\"status\":\"ACTIVE\",\"displayName\":\"reader\"}")));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/v1/posts/1"))
                .willReturn(okJson("{\"postId\":1,\"userKey\":\"owner\"}")));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/posts/1/comments"))
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"commentId\":2,\"postId\":1,\"userKey\":\"reader\",\"content\":\"hello\"}")));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/posts/1/comments").header("X-User-Key", "reader")
                .contentType("application/json").content("{\"content\":\"hello\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.author.displayName").value("reader"));
        verify(postRequestedFor(urlEqualTo("/api/v1/posts/1/comments"))
                .withHeader("X-User-Key", equalTo("reader"))
                .withRequestBody(matchingJsonPath("$.content", equalTo("hello"))));
    }

    @Test void deletionCallsInternalCommentCleanupAndPreservesPostRequester() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/internal/v1/users/owner/author"))
                .willReturn(okJson("{\"status\":\"ACTIVE\",\"displayName\":\"owner\"}")));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/v1/posts/1"))
                .willReturn(okJson("{\"postId\":1,\"userKey\":\"owner\"}")));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.delete(urlEqualTo("/internal/v1/posts/1/comments"))
                .willReturn(aResponse().withStatus(204)));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.delete(urlEqualTo("/api/v1/posts/1"))
                .willReturn(aResponse().withStatus(204)));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/posts/1").header("X-User-Key", "owner")).andExpect(status().isNoContent());
        verify(deleteRequestedFor(urlEqualTo("/internal/v1/posts/1/comments")));
        verify(deleteRequestedFor(urlEqualTo("/api/v1/posts/1")).withHeader("X-User-Key", equalTo("owner")));
    }
}
