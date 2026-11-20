package com.petfeed.facade;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.web.servlet.MockMvc;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
class FeedContractTest {
    @Autowired MockMvc mvc;

    @Test void composesFeedOverRealFeignSerializationAndProtectsMissingIdentity() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathMatching("/internal/v1/members/(feed-viewer|feed-author)/author"))
                .willReturn(okJson("{\"status\":\"ACTIVE\",\"displayName\":\"friend\"}")));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/internal/v1/feed/candidates"))
                .withQueryParam("requester", equalTo("feed-viewer"))
                .willReturn(okJson("[{\"postId\":99,\"memberKey\":\"feed-author\",\"createdAt\":\"2026-11-20T12:00:00Z\"}]")));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/internal/v1/follows/targets"))
                .willReturn(okJson("[\"feed-author\"]")));
        stubFor(post(urlEqualTo("/internal/v1/feed/posts")).withRequestBody(equalToJson("[99]"))
                .willReturn(okJson("[{\"postId\":99,\"memberKey\":\"feed-author\",\"content\":\"hello\",\"mediaIds\":[]}]")));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/internal/v1/comments/counts"))
                .willReturn(okJson("{\"99\":3}")));
        mvc.perform(get("/api/v1/feed").header("X-Member-Key", "feed-viewer"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].post.postId").value(99))
                .andExpect(jsonPath("$.items[0].post.author.displayName").value("friend"))
                .andExpect(jsonPath("$.items[0].commentCount").value(3))
                .andExpect(jsonPath("$.items[0].source").value("FOLLOWING"))
                .andExpect(jsonPath("$.hasNext").value(false));
        mvc.perform(get("/api/v1/feed")).andExpect(status().isBadRequest());
    }
}
