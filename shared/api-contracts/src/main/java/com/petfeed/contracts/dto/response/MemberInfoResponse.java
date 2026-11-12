package com.petfeed.contracts.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Member-owned public information, without cross-service aggregates. */
@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class MemberInfoResponse {
    private String nickname;
    private String profileImageUrl;
    private String bio;
}
