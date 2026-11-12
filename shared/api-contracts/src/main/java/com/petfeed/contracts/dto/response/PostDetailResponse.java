package com.petfeed.contracts.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class PostDetailResponse {
    private Long postId;
    @com.fasterxml.jackson.annotation.JsonProperty("userKey")
    private String memberKey;
    private String content;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuthorResponse author;
    private java.util.List<String> mediaIds = java.util.List.of();

    public static PostDetailResponse of(Long postId, String memberKey, String content, String thumbnailUrl,
            LocalDateTime createdAt, LocalDateTime updatedAt, AuthorResponse author) {
        return of(postId, memberKey, content, thumbnailUrl, createdAt, updatedAt, author, java.util.List.of());
    }

    public PostDetailResponse withAuthor(AuthorResponse author) {
        return of(postId, memberKey, content, thumbnailUrl, createdAt, updatedAt, author, mediaIds);
    }
}
