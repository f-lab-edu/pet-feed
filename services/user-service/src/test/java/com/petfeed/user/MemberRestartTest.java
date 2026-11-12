package com.petfeed.user;

import com.petfeed.user.domain.Member;
import com.petfeed.user.repository.MemberRepository;
import com.petfeed.domain.dto.response.MemberStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class MemberRestartTest {
    @TempDir Path directory;

    @Test
    void memberIdentityAndWithdrawalSurviveRestart() {
        String key;
        try (var context = start()) {
            var member = Member.create("owner@example.com", "hash", "01012345678", "owner");
            member.withdraw();
            key = context.getBean(MemberRepository.class).saveAndFlush(member).getUserKey();
        }
        try (var context = start()) {
            var member = context.getBean(MemberRepository.class).findById(key).orElseThrow();
            assertThat(member.getEmail()).isEqualTo("owner@example.com");
            assertThat(member.getPasswordHash()).isEqualTo("hash");
            assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        }
    }

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(UserServiceApplication.class).run("--server.port=0",
                "--spring.datasource.url=jdbc:h2:file:" + directory.resolve("member").toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE");
    }
}
