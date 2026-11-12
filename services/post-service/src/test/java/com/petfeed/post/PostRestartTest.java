package com.petfeed.post;

import com.petfeed.post.domain.Post;
import com.petfeed.post.repository.CommentCleanupRepository;
import com.petfeed.post.repository.PostRepository;
import com.petfeed.post.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PostRestartTest {
    @TempDir Path directory;

    @Test
    void attachmentsAndFailedCleanupSurviveRestartAndRetryCompletes() {
        long visibleId;
        long deletedId;
        var mediaIds = List.of("00000000-0000-0000-0000-000000000002", "00000000-0000-0000-0000-000000000001");
        try (var context = start()) {
            var posts = context.getBean(PostRepository.class);
            var visible = Post.create("owner", "with attachments", null);
            visible.replaceMedia(mediaIds);
            visibleId = posts.saveAndFlush(visible).getId();
            deletedId = posts.saveAndFlush(Post.create("owner", "deleted", null)).getId();
            context.getBean(PostService.class).delete(deletedId, "owner");
            doThrow(new ResourceAccessException("offline")).when(context.getBean(CommentCleanupClient.class)).close(deletedId);
            context.getBean(CommentCleanupProcessor.class).process(deletedId);
        }
        try (var context = start()) {
            assertThat(context.getBean(PostService.class).get(visibleId).getMediaIds()).containsExactlyElementsOf(mediaIds);
            assertThat(context.getBean(PostRepository.class).findById(deletedId).orElseThrow().isDeleted()).isTrue();
            var tasks = context.getBean(CommentCleanupRepository.class);
            var task = tasks.findById(deletedId).orElseThrow();
            assertThat(task.getAttempts()).isEqualTo(1);
            context.getBean(JdbcTemplate.class).update(
                    "update comment_cleanup_tasks set next_attempt_at = current_timestamp where post_id = ?", deletedId);
            new CommentCleanupScheduler(tasks, context.getBean(CommentCleanupProcessor.class)).poll();
            verify(context.getBean(CommentCleanupClient.class)).close(deletedId);
            assertThat(tasks.existsById(deletedId)).isFalse();
        }
        try (var context = start()) {
            assertThat(context.getBean(CommentCleanupRepository.class).existsById(deletedId)).isFalse();
            assertThat(context.getBean(PostService.class).get(visibleId).getMediaIds()).containsExactlyElementsOf(mediaIds);
        }
    }

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(PostServiceApplication.class, CleanupStub.class).run(
                "--server.port=0", "--post.cleanup.enabled=false",
                "--spring.datasource.url=jdbc:h2:file:" + directory.resolve("post").toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CleanupStub {
        @Bean @Primary
        CommentCleanupClient restartCleanupClient() { return mock(CommentCleanupClient.class); }
    }
}
