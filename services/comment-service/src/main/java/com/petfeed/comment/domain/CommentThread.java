package com.petfeed.comment.domain;

import jakarta.persistence.*;
import lombok.*;

/** Durable deletion fence: closing a post's comments is irreversible. */
@Entity
@Table(name = "comment_threads")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentThread {
    @Id
    private Long postId;
    @Column(nullable = false)
    private boolean closed;

    public CommentThread(Long postId) { this.postId = postId; }
    public void close() { closed = true; }
}
