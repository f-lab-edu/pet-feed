package com.petfeed.domain.dto.response;

import lombok.*;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class FollowPage {
    private List<FollowEntry> entries;
    private Long nextCursor;
    private boolean hasNext;
}
