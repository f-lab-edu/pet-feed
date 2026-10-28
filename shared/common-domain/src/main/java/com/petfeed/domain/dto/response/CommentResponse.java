package com.petfeed.domain.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class CommentResponse {
    private Long commentId;
    private Long postId;
    private String userKey;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuthorResponse author;

    public CommentResponse withAuthor(AuthorResponse author) {
        return of(commentId, postId, userKey, content, createdAt, updatedAt, author);
    }
}
