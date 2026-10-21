package com.petfeed.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostListResponse {

    private List<PostSummary> posts;
    private Long nextCursor;
    private boolean hasNext;
}
