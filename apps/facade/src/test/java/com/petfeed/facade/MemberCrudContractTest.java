package com.petfeed.facade;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
class MemberCrudContractTest {
    @Autowired MockMvc mvc;

    @Test
    void signupReturnsCreated() throws Exception {
        stubFor(post(urlEqualTo("/api/v1/users/signup")).willReturn(okJson("{\"userKey\":\"new-member\",\"createdAt\":\"2026-10-22T00:00:00\"}")));
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/users/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"owner@example.com\",\"password\":\"secret123!\",\"phone\":\"01012345678\",\"nickname\":\"owner\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userKey").value("new-member"));
    }

    @Test
    void updateForwardsAuthenticatedRequesterAndBody() throws Exception {
        stubFor(put(urlEqualTo("/api/v1/users/update-owner")).willReturn(noContent()));
        mvc.perform(MockMvcRequestBuilders.put("/api/v1/users/update-owner").header("X-User-Key", "update-owner")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bio\":\"new bio\"}"))
                .andExpect(status().isNoContent());
        verify(putRequestedFor(urlEqualTo("/api/v1/users/update-owner"))
                .withHeader("X-User-Key", equalTo("update-owner"))
                .withRequestBody(matchingJsonPath("$.bio", equalTo("new bio"))));
    }

    @Test
    void deleteForwardsAuthenticatedRequester() throws Exception {
        stubFor(delete(urlEqualTo("/api/v1/users/delete-owner")).willReturn(noContent()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/users/delete-owner")
                        .header("X-User-Key", "delete-owner"))
                .andExpect(status().isNoContent());
        verify(deleteRequestedFor(urlEqualTo("/api/v1/users/delete-owner"))
                .withHeader("X-User-Key", equalTo("delete-owner")));
    }

    @Test
    void downstreamErrorsKeepTheirStatus() throws Exception {
        stubFor(post(urlEqualTo("/api/v1/auth/login")).willReturn(aResponse().withStatus(401)));
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"owner@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        stubFor(delete(urlEqualTo("/api/v1/users/forbidden-owner")).willReturn(aResponse().withStatus(403)));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/users/forbidden-owner")
                        .header("X-User-Key", "other"))
                .andExpect(status().isForbidden());
    }
}
