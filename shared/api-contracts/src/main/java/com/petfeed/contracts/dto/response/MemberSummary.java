package com.petfeed.contracts.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class MemberSummary {

    @com.fasterxml.jackson.annotation.JsonProperty("userKey")
    private String memberKey;
    private String nickname;
    private String profileImageUrl;
    private boolean isFollowing;
}
