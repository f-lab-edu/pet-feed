package com.petfeed.contracts.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class MemberSummary {

    private String memberKey;
    private String nickname;
    private String profileImageUrl;
    @com.fasterxml.jackson.annotation.JsonProperty("following")
    private boolean isFollowing;
}
