package com.petfeed.post.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "comments", indexes = @Index(name = "idx_comment_post_id", columnList = "post_id,id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    private Post post;
    @Column(nullable = false, updatable = false)
    private String userKey;
    @Column(nullable = false, length = 1000)
    private String content;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static Comment create(Post post, String key, String content) {
        if (key == null || key.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Author is required");
        Comment comment = new Comment();
        comment.post = post;
        comment.userKey = key;
        comment.createdAt = LocalDateTime.now();
        comment.replace(content);
        return comment;
    }

    public void requireOwner(String key) {
        if (!userKey.equals(key))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can change this comment");
    }

    public void update(String key, String content) {
        requireOwner(key);
        replace(content);
    }

    private void replace(String content) {
        if (content == null || content.isBlank() || content.length() > 1000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid comment content");
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}
