package com.petfeed.post;

import com.petfeed.post.domain.Post;
import com.petfeed.post.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "post.cleanup.enabled=false")
@AutoConfigureMockMvc
@Transactional
class FeedQueryTest {
    @Autowired MockMvc mvc;
    @Autowired PostRepository posts;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test void excludesOwnAndDeletedPostsAndBatchReadRechecksDeletion() throws Exception {
        posts.deleteAll();
        var own = posts.save(Post.create("viewer", "own", null));
        var visible = posts.save(Post.create("other", "visible", null));
        var deleted = Post.create("other", "deleted", null);
        deleted.markDeleted();
        posts.saveAndFlush(deleted);
        var old = posts.saveAndFlush(Post.create("other", "old", null));
        jdbc.update("update posts set created_at = ? where id = ?",
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusDays(31)), old.getId());
        mvc.perform(get("/internal/v1/feed/candidates").param("requester", "viewer"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].postId").value(visible.getId()));
        visible.markDeleted();
        posts.flush();
        mvc.perform(post("/internal/v1/feed/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("[" + visible.getId() + "," + own.getId() + "]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("own"));
        mvc.perform(post("/internal/v1/feed/posts").contentType(MediaType.APPLICATION_JSON).content("[0]"))
                .andExpect(status().isBadRequest());
    }

    @Test void candidatePoolIsBoundedToLatestFiveHundredPosts() throws Exception {
        posts.deleteAll();
        Long newest = null;
        for (int i = 0; i < 501; i++) newest = posts.save(Post.create("other", "post " + i, null)).getId();
        posts.flush();
        mvc.perform(get("/internal/v1/feed/candidates").param("requester", "viewer"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(500))
                .andExpect(jsonPath("$[0].postId").value(newest));
    }
}
