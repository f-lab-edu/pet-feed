package com.petfeed.facade;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.web.servlet.MockMvc;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
class MediaContractTest {
    @Autowired MockMvc mvc;
    static final String ID = "c6bc523f-e474-42b0-9e57-7a4fe02c6785";

    @Test void uploadForwardsBytesAndAuthenticatedOwner() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/internal/v1/users/uploader/author"))
                .willReturn(okJson("{\"status\":\"ACTIVE\",\"displayName\":\"owner\"}")));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/internal/v1/media"))
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"mediaId\":\"" + ID + "\",\"contentType\":\"video/mp4\",\"size\":3}")));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/media").header("X-User-Key", "uploader").contentType("video/mp4")
                .content(new byte[]{1, 2, 3})).andExpect(status().isCreated()).andExpect(jsonPath("$.mediaId").value(ID));
        verify(postRequestedFor(urlEqualTo("/internal/v1/media")).withHeader("X-User-Key", equalTo("uploader"))
                .withHeader("Content-Type", equalTo("video/mp4")).withRequestBody(binaryEqualTo(new byte[]{1, 2, 3})));
    }

    @Test void foreignAttachmentPreventsPostWrite() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/internal/v1/users/foreign-media-user/author"))
                .willReturn(okJson("{\"status\":\"ACTIVE\",\"displayName\":\"owner\"}")));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/internal/v1/media/validate"))
                .willReturn(aResponse().withStatus(403)));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/posts").header("X-User-Key", "foreign-media-user").contentType("application/json")
                .content("{\"content\":\"hello\",\"mediaIds\":[\"" + ID + "\"]}"))
                .andExpect(status().isForbidden());
        verify(0, postRequestedFor(urlEqualTo("/api/v1/posts")).withHeader("X-User-Key", equalTo("foreign-media-user")));
    }

    @Test void contentRequiresVisiblePostAndMembershipAndPreservesRange() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/v1/posts/501"))
                .willReturn(okJson("{\"postId\":501,\"mediaIds\":[\"" + ID + "\"]}")));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/internal/v1/media/" + ID + "/content"))
                .withHeader("Range", equalTo("bytes=0-2")).willReturn(aResponse().withStatus(206)
                        .withHeader("Content-Type", "video/mp4").withHeader("Content-Range", "bytes 0-2/10")
                        .withHeader("Cache-Control", "no-store").withBody(new byte[]{1, 2, 3})));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/posts/501/media/" + ID).header("Range", "bytes=0-2"))
                .andExpect(status().isPartialContent()).andExpect(header().string("Content-Range", "bytes 0-2/10"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/posts/501/media/another-id")).andExpect(status().isNotFound());
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/v1/posts/501"))
                .willReturn(aResponse().withStatus(404)));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/posts/501/media/" + ID)).andExpect(status().isNotFound());
        verify(1, getRequestedFor(urlEqualTo("/internal/v1/media/" + ID + "/content")));
    }
}
