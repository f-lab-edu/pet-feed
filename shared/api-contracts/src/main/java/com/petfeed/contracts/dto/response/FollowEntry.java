package com.petfeed.contracts.dto.response;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class FollowEntry {
    private Long followId;
    @com.fasterxml.jackson.annotation.JsonProperty("userKey")
    private String memberKey;
}
