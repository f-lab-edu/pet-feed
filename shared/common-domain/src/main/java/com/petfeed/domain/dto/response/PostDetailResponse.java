package com.petfeed.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class PostDetailResponse {
    private Long postId;
    private String userKey;
    private String content;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuthorResponse author;

    public PostDetailResponse withAuthor(AuthorResponse author) {
        return of(postId, userKey, content, thumbnailUrl, createdAt, updatedAt, author);
    }
}
