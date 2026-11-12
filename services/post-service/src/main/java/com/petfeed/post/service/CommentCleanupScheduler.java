package com.petfeed.post.service;

import com.petfeed.post.repository.CommentCleanupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Instant;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "post.cleanup.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class CommentCleanupScheduler {
    private final CommentCleanupRepository tasks;
    private final CommentCleanupProcessor processor;

    @Scheduled(fixedDelayString = "${post.cleanup.poll-delay-ms:5000}")
    public void poll() {
        for (var task : tasks.findTop20ByNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(Instant.now())) {
            try {
                processor.process(task.getPostId());
            } catch (RuntimeException failure) {
                log.error("Comment cleanup transaction failed: postId={}", task.getPostId(), failure);
            }
        }
    }
}
