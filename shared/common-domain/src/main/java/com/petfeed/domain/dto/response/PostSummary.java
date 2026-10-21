package com.petfeed.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class PostSummary {

    private Long postId;
    private String thumbnailUrl;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuthorResponse author;

    public PostSummary withAuthor(AuthorResponse author) {
        return PostSummary.of(postId, thumbnailUrl, likeCount, commentCount, createdAt, updatedAt, author);
    }
}
