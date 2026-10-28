package com.petfeed.domain.dto.response;

import lombok.*;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class CommentListResponse {
    private List<CommentResponse> comments;
    private Long nextCursor;
    private boolean hasNext;
}
