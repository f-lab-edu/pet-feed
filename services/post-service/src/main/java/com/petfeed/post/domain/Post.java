package com.petfeed.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "posts", indexes = @Index(name = "idx_post_author_id", columnList = "userKey,id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, updatable = false)
    private String userKey;
    @Column(nullable = false, length = 5000)
    private String content;
    @Column(length = 2048)
    private String thumbnailUrl;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static Post create(String userKey, String content, String thumbnailUrl) {
        if (userKey == null || userKey.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Author is required");
        Post post = new Post();
        post.userKey = userKey;
        post.createdAt = LocalDateTime.now();
        post.replace(content, thumbnailUrl);
        return post;
    }

    public void requireOwner(String requesterKey) {
        if (!userKey.equals(requesterKey))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can change this post");
    }

    public void update(String requesterKey, String content, String thumbnailUrl) {
        requireOwner(requesterKey);
        replace(content, thumbnailUrl);
    }

    private void replace(String content, String thumbnailUrl) {
        if (content == null || content.isBlank() || content.length() > 5000
                || (thumbnailUrl != null && thumbnailUrl.length() > 2048))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid post content or thumbnail URL");
        this.content = content;
        this.thumbnailUrl = thumbnailUrl;
        this.updatedAt = LocalDateTime.now();
    }
}
