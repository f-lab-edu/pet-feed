package com.petfeed.post.service;

import com.petfeed.post.repository.CommentCleanupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentCleanupProcessor {
    private final CommentCleanupRepository tasks;
    private final CommentCleanupClient client;

    // A bounded HTTP call holds the task lock; duplicate delivery remains safe on the consumer.
    @Transactional
    public void process(Long postId) {
        var task = tasks.lockById(postId).orElse(null);
        if (task == null || task.getNextAttemptAt().isAfter(Instant.now())) return;
        try {
            client.close(postId);
        } catch (org.springframework.web.client.RestClientException failure) {
            task.retryAfter(30);
            log.warn("Comment cleanup failed: postId={}, attempts={}", postId, task.getAttempts(), failure);
            return;
        }
        tasks.delete(task);
    }
}
