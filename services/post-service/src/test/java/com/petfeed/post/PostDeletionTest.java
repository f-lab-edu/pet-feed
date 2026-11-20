package com.petfeed.post;

import com.petfeed.post.domain.Post;
import com.petfeed.post.repository.*;
import com.petfeed.post.service.*;
import com.petfeed.contracts.dto.request.SavePostRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.ResourceAccessException;
import java.time.Instant;
import java.sql.Timestamp;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "post.cleanup.enabled=false")
class PostDeletionTest {
    @Autowired PostService service;
    @Autowired PostRepository posts;
    @Autowired CommentCleanupRepository tasks;
    @Autowired CommentCleanupProcessor processor;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean CommentCleanupClient client;

    @BeforeEach void reset() { tasks.deleteAll(); posts.deleteAll(); }

    @Test void deletionHidesPostAndQueuesCleanupExactlyOnce() {
        long id = posts.save(Post.create("owner", "hello", null)).getId();
        service.delete(id, "owner");
        service.delete(id, "owner");
        assertThat(tasks.count()).isEqualTo(1);
        assertThat(posts.findById(id).orElseThrow().isDeleted()).isTrue();
        assertThat(service.count("owner")).isZero();
        assertThat(service.list("owner", 0L).getPosts()).isEmpty();
        assertThat(service.list("owner", id + 1).getPosts()).isEmpty();
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> service.update(id, "owner", SavePostRequest.of("changed", null)))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> service.delete(id, "other"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(client);
    }

    @Test void rollbackDoesNotLeaveDeletedPostOrCleanupTask() {
        long id = posts.save(Post.create("owner", "hello", null)).getId();
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            service.delete(id, "owner");
            posts.flush();
            tasks.flush();
            tx.setRollbackOnly();
        });
        assertThat(service.get(id).getContent()).isEqualTo("hello");
        assertThat(tasks.count()).isZero();
    }

    @Test void concurrentDeletionQueuesOneTask() throws Exception {
        long id = posts.save(Post.create("owner", "hello", null)).getId();
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 4; i++) futures.add(executor.submit(() -> {
                start.await();
                service.delete(id, "owner");
                return null;
            }));
            start.countDown();
            for (var future : futures) future.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(tasks.count()).isEqualTo(1);
    }

    @Test void unavailableConsumerRetainsTaskAndLaterRetryCompletes() {
        long id = posts.save(Post.create("owner", "hello", null)).getId();
        service.delete(id, "owner");
        doThrow(new ResourceAccessException("response lost")).doNothing().when(client).close(id);
        processor.process(id);
        var failed = tasks.findById(id).orElseThrow();
        assertThat(failed.getAttempts()).isEqualTo(1);
        assertThat(failed.getNextAttemptAt()).isAfter(Instant.now());
        processor.process(id);
        verify(client, times(1)).close(id);
        jdbc.update("update comment_cleanup_tasks set next_attempt_at = ? where post_id = ?",
                Timestamp.from(Instant.now().minusSeconds(1)), id);
        new CommentCleanupScheduler(tasks, processor).poll();
        processor.process(id);
        assertThat(tasks.existsById(id)).isFalse();
        verify(client, times(2)).close(id);
        service.delete(id, "owner");
        assertThat(tasks.count()).isZero();
    }

    @Test void lostAcknowledgementReplaysTaskAfterTransactionRollback() {
        long id = posts.save(Post.create("owner", "hello", null)).getId();
        service.delete(id, "owner");
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            processor.process(id);
            tasks.flush();
            tx.setRollbackOnly();
        });
        assertThat(tasks.existsById(id)).isTrue();
        processor.process(id);
        verify(client, times(2)).close(id);
        assertThat(tasks.existsById(id)).isFalse();
    }
}
