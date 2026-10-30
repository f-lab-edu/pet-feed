package com.petfeed.domain.dto.response;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class FollowEntry {
    private Long followId;
    private String userKey;
}
