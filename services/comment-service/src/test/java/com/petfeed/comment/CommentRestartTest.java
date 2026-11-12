package com.petfeed.comment;

import com.petfeed.comment.service.CommentService;
import com.petfeed.domain.dto.request.SaveCommentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class CommentRestartTest {
    @TempDir Path directory;

    @Test
    void commentsAndClosedThreadsSurviveRestart() {
        try (var context = start()) {
            var service = context.getBean(CommentService.class);
            service.create(1L, "owner", SaveCommentRequest.of("preserved"));
            service.create(2L, "owner", SaveCommentRequest.of("deleted"));
            service.close(2L);
        }
        try (var context = start()) {
            var service = context.getBean(CommentService.class);
            assertThat(service.counts(java.util.List.of(1L)).get(1L)).isEqualTo(1L);
            assertThat(service.counts(java.util.List.of(2L))).isEmpty();
            assertThatThrownBy(() -> service.create(2L, "owner", SaveCommentRequest.of("late retry")))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            error -> assertThat(error.getStatusCode().value()).isEqualTo(404));
            service.close(2L);
        }
    }

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(CommentServiceApplication.class).run("--server.port=0",
                "--spring.datasource.url=jdbc:h2:file:" + directory.resolve("comment").toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE");
    }
}
