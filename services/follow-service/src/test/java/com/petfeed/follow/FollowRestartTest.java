package com.petfeed.follow;

import com.petfeed.follow.repository.FollowRepository;
import com.petfeed.follow.service.FollowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class FollowRestartTest {
    @TempDir Path directory;

    @Test
    void followingSurvivesRestartAndDuplicateIsStillRejected() {
        try (var context = start()) {
            context.getBean(FollowService.class).follow("owner", "target");
        }
        try (var context = start()) {
            assertThat(context.getBean(FollowRepository.class).existsByFollowerKeyAndFollowingKey("owner", "target")).isTrue();
            assertThatThrownBy(() -> context.getBean(FollowService.class).follow("owner", "target"))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            error -> assertThat(error.getStatusCode().value()).isEqualTo(409));
        }
    }

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(FollowServiceApplication.class).run("--server.port=0",
                "--spring.datasource.url=jdbc:h2:file:" + directory.resolve("follow").toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE");
    }
}
