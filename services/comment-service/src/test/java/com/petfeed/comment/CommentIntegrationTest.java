package com.petfeed.comment;

import com.petfeed.comment.domain.Comment;
import com.petfeed.comment.repository.*;
import com.petfeed.comment.service.CommentService;
import com.petfeed.domain.dto.request.SaveCommentRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CommentIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CommentRepository comments;
    @Autowired CommentThreadRepository threads;
    @Autowired CommentService service;
    @BeforeEach void reset() { comments.deleteAll(); threads.deleteAll(); }

    @Test void lifecycleUsesPostIdWithoutPostDatabase() throws Exception {
        String path = "/api/v1/posts/12/comments";
        mvc.perform(post(path).header("X-User-Key", "reader").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"hello\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.postId").value(12));
        long id = comments.findAll().getFirst().getId();
        mvc.perform(put(path + "/" + id).header("X-User-Key", "reader").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"updated\"}")).andExpect(status().isNoContent());
        mvc.perform(get(path)).andExpect(jsonPath("$.comments[0].content").value("updated"));
        mvc.perform(delete(path + "/" + id).header("X-User-Key", "reader")).andExpect(status().isNoContent());
        assertThat(comments.existsById(id)).isFalse();
    }

    @Test void ownershipValidationAndPostIsolation() throws Exception {
        long id = comments.save(Comment.create(12L, "reader", "original")).getId();
        String path = "/api/v1/posts/12/comments";
        mvc.perform(put(path + "/" + id).header("X-User-Key", "other").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"changed\"}")).andExpect(status().isForbidden());
        mvc.perform(delete(path + "/" + id).header("X-User-Key", "other")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/posts/13/comments/" + id).header("X-User-Key", "reader")).andExpect(status().isNotFound());
        for (String content : new String[]{" ", "x".repeat(1001)}) {
            mvc.perform(post(path).header("X-User-Key", "reader").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"" + content + "\"}")).andExpect(status().isBadRequest());
        }
        mvc.perform(get(path).param("cursor", "-1")).andExpect(status().isBadRequest());
        assertThat(comments.findById(id).orElseThrow().getContent()).isEqualTo("original");
    }

    @Test void cursorAndBatchCountsAreIsolatedByPost() throws Exception {
        var ids = new java.util.ArrayList<Long>();
        for (int i = 0; i < 21; i++) ids.add(comments.save(Comment.create(12L, "reader", "comment " + i)).getId());
        comments.save(Comment.create(13L, "reader", "other"));
        mvc.perform(get("/api/v1/posts/12/comments")).andExpect(jsonPath("$.comments.length()").value(20))
                .andExpect(jsonPath("$.comments[0].commentId").value(ids.getLast()))
                .andExpect(jsonPath("$.nextCursor").value(ids.get(1))).andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/api/v1/posts/12/comments").param("cursor", ids.get(1).toString()))
                .andExpect(jsonPath("$.comments.length()").value(1)).andExpect(jsonPath("$.hasNext").value(false));
        mvc.perform(get("/internal/v1/comments/counts").param("postIds", "12,13,14"))
                .andExpect(jsonPath("$['12']").value(21)).andExpect(jsonPath("$['13']").value(1))
                .andExpect(jsonPath("$['14']").doesNotExist());
    }

    @Test void closeIsIdempotentAndRejectsLateCreation() throws Exception {
        comments.save(Comment.create(12L, "reader", "removed"));
        comments.save(Comment.create(13L, "reader", "retained"));
        for (int i = 0; i < 2; i++) mvc.perform(delete("/internal/v1/posts/12/comments")).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/posts/12/comments").header("X-User-Key", "reader")
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"late\"}")).andExpect(status().isNotFound());
        assertThat(comments.countByPostId(12L)).isZero();
        assertThat(comments.countByPostId(13L)).isEqualTo(1);
    }

    @Test void concurrentFirstCreationAndCloseCannotLeaveOrphanComments() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        try {
            var tasks = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 7; i++) tasks.add(executor.submit(() -> {
                start.await();
                try { service.create(99L, "reader", SaveCommentRequest.of("racing")); }
                catch (ResponseStatusException closed) { assertThat(closed.getStatusCode().value()).isEqualTo(404); }
                return null;
            }));
            tasks.add(executor.submit(() -> { start.await(); service.close(99L); return null; }));
            start.countDown();
            for (var task : tasks) task.get(20, TimeUnit.SECONDS);
            assertThat(comments.countByPostId(99L)).isZero();
            assertThat(threads.findById(99L).orElseThrow().isClosed()).isTrue();
        } finally { executor.shutdownNow(); }
    }
}