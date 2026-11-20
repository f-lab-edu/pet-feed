package com.petfeed.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "posts", indexes = @Index(name = "idx_post_author_id", columnList = "user_key,id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {
    @Version
    private long version;
    @Column(nullable = false)
    private boolean deleted;

    @ElementCollection
    @CollectionTable(name = "post_media", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "display_order")
    @Column(name = "media_id", nullable = false, length = 36)
    private java.util.List<String> mediaIds = new java.util.ArrayList<>();

    public void replaceMedia(java.util.List<String> ids) {
        if (ids == null) return;
        if (ids.size() > 6 || new java.util.HashSet<>(ids).size() != ids.size()
                || ids.stream().anyMatch(id -> id == null || !id.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attachments");
        mediaIds.clear();
        mediaIds.addAll(ids);
    }

    public void markDeleted() { deleted = true; }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_key", nullable = false, updatable = false)
    private String memberKey;
    @Column(nullable = false, length = 5000)
    private String content;
    @Column(length = 2048)
    private String thumbnailUrl;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static Post create(String memberKey, String content, String thumbnailUrl) {
        if (memberKey == null || memberKey.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Author is required");
        Post post = new Post();
        post.memberKey = memberKey;
        post.createdAt = LocalDateTime.now();
        post.replace(content, thumbnailUrl);
        return post;
    }

    public void requireOwner(String requesterKey) {
        if (!memberKey.equals(requesterKey))
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
