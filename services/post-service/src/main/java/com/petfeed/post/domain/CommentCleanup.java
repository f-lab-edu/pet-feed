package com.petfeed.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "comment_cleanup_tasks", indexes = @Index(name = "idx_cleanup_due", columnList = "nextAttemptAt"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentCleanup {
    @Id
    private Long postId;
    @Column(nullable = false)
    private Instant nextAttemptAt;
    private long attempts;

    public CommentCleanup(Long postId) {
        this.postId = postId;
        this.nextAttemptAt = Instant.now();
    }

    public void retryAfter(long seconds) {
        attempts++;
        nextAttemptAt = Instant.now().plusSeconds(seconds);
    }
}
