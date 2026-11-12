package com.petfeed.post;

import com.petfeed.post.domain.Post;
import com.petfeed.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "post.cleanup.enabled=false")
@AutoConfigureMockMvc
class PostIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired PostRepository posts;
    @BeforeEach void reset() { posts.deleteAll(); }

    @Test void lifecyclePersistsUpdatesAndDeletion() throws Exception {
        mvc.perform(post("/api/v1/posts").header("X-User-Key", "owner")
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hello\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userKey").value("owner"));
        long id = posts.findAll().getFirst().getId();
        mvc.perform(put("/api/v1/posts/{id}", id).header("X-User-Key", "owner")
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"updated\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/posts/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("updated"));
        assertThat(posts.findById(id).orElseThrow().getMemberKey()).isEqualTo("owner");
        mvc.perform(delete("/api/v1/posts/{id}", id).header("X-User-Key", "owner"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/posts/{id}", id)).andExpect(status().isNotFound());
    }

    @Test void invalidAndUnauthorizedWritesLeaveStoredPostIntact() throws Exception {
        long id = posts.save(Post.create("owner", "original", null)).getId();
        mvc.perform(put("/api/v1/posts/{id}", id).header("X-User-Key", "other")
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"changed\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/posts/{id}", id).header("X-User-Key", "other"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/posts").header("X-User-Key", "owner")
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/posts/{id}", id)).andExpect(status().isBadRequest());
        assertThat(posts.findById(id).orElseThrow().getContent()).isEqualTo("original");
        assertThat(posts.count()).isEqualTo(1);
    }

    @Test void cursorPagesAreDescendingAndIsolatedByAuthor() throws Exception {
        for (int i = 0; i < 21; i++) posts.save(Post.create("owner", "post " + i, null));
        posts.save(Post.create("other", "other", null));
        var ids = posts.findAll().stream().filter(p -> p.getMemberKey().equals("owner"))
                .map(Post::getId).sorted(java.util.Comparator.reverseOrder()).toList();
        mvc.perform(get("/api/v1/users/owner/posts")).andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(20))
                .andExpect(jsonPath("$.posts[0].postId").value(ids.getFirst()))
                .andExpect(jsonPath("$.nextCursor").value(ids.get(19)))
                .andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/api/v1/users/owner/posts").param("cursor", ids.get(19).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.posts[0].postId").value(ids.getLast()))
                .andExpect(jsonPath("$.hasNext").value(false));
        mvc.perform(get("/api/v1/posts/count/owner")).andExpect(content().string("21"));
        mvc.perform(get("/api/v1/users/missing/posts")).andExpect(jsonPath("$.posts.length()").value(0));
        mvc.perform(get("/api/v1/users/owner/posts").param("cursor", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test void attachmentsPreserveOrderAndCanBeReplacedOrRemoved() throws Exception {
        String first = "c6bc523f-e474-42b0-9e57-7a4fe02c6785";
        String second = "25d37d69-b8d8-4a73-ac35-e50383f72190";
        mvc.perform(post("/api/v1/posts").header("X-User-Key", "owner").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"photos\",\"mediaIds\":[\"" + first + "\",\"" + second + "\"]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mediaIds[0]").value(first))
                .andExpect(jsonPath("$.mediaIds[1]").value(second));
        long id = posts.findAll().getFirst().getId();
        mvc.perform(put("/api/v1/posts/{id}", id).header("X-User-Key", "owner").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"text only edit\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/posts/{id}", id)).andExpect(jsonPath("$.mediaIds.length()").value(2));
        mvc.perform(put("/api/v1/posts/{id}", id).header("X-User-Key", "owner").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"one attachment\",\"mediaIds\":[\"" + second + "\"]}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/posts/{id}", id)).andExpect(jsonPath("$.mediaIds[0]").value(second))
                .andExpect(jsonPath("$.mediaIds.length()").value(1));
        mvc.perform(put("/api/v1/posts/{id}", id).header("X-User-Key", "owner").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"removed\",\"mediaIds\":[]}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/posts/{id}", id)).andExpect(jsonPath("$.mediaIds").isEmpty());
        mvc.perform(post("/api/v1/posts").header("X-User-Key", "owner").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"duplicate\",\"mediaIds\":[\"" + first + "\",\"" + first + "\"]}"))
                .andExpect(status().isBadRequest());
    }
}
