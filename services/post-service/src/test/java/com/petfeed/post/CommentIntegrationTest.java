package com.petfeed.post;

import com.petfeed.post.domain.*;
import com.petfeed.post.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired PostRepository posts;
    @Autowired CommentRepository comments;

    @Test void lifecycleAndPostDeletion() throws Exception {
        long postId = posts.save(Post.create("owner", "post", null)).getId();
        String path = "/api/v1/posts/" + postId + "/comments";
        mvc.perform(post(path).header("X-User-Key", "reader").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"hello\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userKey").value("reader"));
        long id = comments.findByPostIdOrderByIdDesc(postId, org.springframework.data.domain.PageRequest.of(0, 1)).getFirst().getId();
        mvc.perform(put(path + "/" + id).header("X-User-Key", "reader").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"updated\"}")).andExpect(status().isNoContent());
        mvc.perform(get(path)).andExpect(jsonPath("$.comments[0].content").value("updated"));
        mvc.perform(get("/api/v1/users/owner/posts")).andExpect(jsonPath("$.posts[0].commentCount").value(1));
        mvc.perform(delete(path + "/" + id).header("X-User-Key", "reader")).andExpect(status().isNoContent());
        assertThat(comments.existsById(id)).isFalse();
        comments.saveAndFlush(Comment.create(posts.findById(postId).orElseThrow(), "reader", "another"));
        mvc.perform(delete("/api/v1/posts/" + postId).header("X-User-Key", "owner")).andExpect(status().isNoContent());
        posts.flush();
        assertThat(comments.countByPostId(postId)).isZero();
    }

    @Test void invalidUnauthorizedAndCrossPostRequestsAreRejected() throws Exception {
        var post = posts.save(Post.create("owner", "post", null));
        var other = posts.save(Post.create("owner", "other", null));
        long id = comments.save(Comment.create(post, "reader", "original")).getId();
        String path = "/api/v1/posts/" + post.getId() + "/comments";
        mvc.perform(put(path + "/" + id).header("X-User-Key", "owner").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"changed\"}")).andExpect(status().isForbidden());
        mvc.perform(delete(path + "/" + id).header("X-User-Key", "owner")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/posts/" + other.getId() + "/comments/" + id)
                .header("X-User-Key", "reader")).andExpect(status().isNotFound());
        for (String content : new String[]{" ", "x".repeat(1001)}) {
            mvc.perform(post(path).header("X-User-Key", "reader").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"" + content + "\"}")).andExpect(status().isBadRequest());
        }
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hello\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/posts/9223372036854775807/comments")).andExpect(status().isNotFound());
        mvc.perform(get(path).param("cursor", "-1")).andExpect(status().isBadRequest());
        assertThat(comments.findById(id).orElseThrow().getContent()).isEqualTo("original");
    }

    @Test void cursorPagesAreDescendingAndIsolatedByPost() throws Exception {
        var post = posts.save(Post.create("owner", "post", null));
        var other = posts.save(Post.create("owner", "other", null));
        var ids = new java.util.ArrayList<Long>();
        for (int i = 0; i < 21; i++) ids.add(comments.save(Comment.create(post, "reader", "comment " + i)).getId());
        comments.save(Comment.create(other, "reader", "other"));
        String path = "/api/v1/posts/" + post.getId() + "/comments";
        mvc.perform(get(path)).andExpect(jsonPath("$.comments.length()").value(20))
                .andExpect(jsonPath("$.comments[0].commentId").value(ids.getLast()))
                .andExpect(jsonPath("$.nextCursor").value(ids.get(1)))
                .andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get(path).param("cursor", ids.get(1).toString()))
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].commentId").value(ids.getFirst()))
                .andExpect(jsonPath("$.hasNext").value(false));
    }
}
