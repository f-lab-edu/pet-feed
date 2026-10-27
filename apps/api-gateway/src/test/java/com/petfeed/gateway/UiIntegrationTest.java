package com.petfeed.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UiIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void anonymousVisitorsCanOpenUiAndAssets() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/index.html"));
        mvc.perform(get("/ui/index.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("petfeed")));
        mvc.perform(get("/ui/app.js")).andExpect(status().isOk())
                .andExpect(content().string(containsString("loadPosts")));
        mvc.perform(get("/ui/style.css")).andExpect(status().isOk());
    }

    @Test void openingUiDoesNotOpenProtectedApiRoutes() throws Exception {
        mvc.perform(get("/api/v1/posts/1")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/posts").contentType("application/json")
                .content("{\"content\":\"hello\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/posts/1/comments/1")).andExpect(status().isForbidden());
    }
}
