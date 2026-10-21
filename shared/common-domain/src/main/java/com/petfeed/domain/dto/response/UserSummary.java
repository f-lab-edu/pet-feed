package com.petfeed.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {

    private String userKey;
    private String nickname;
    private String profileImageUrl;
    private boolean isFollowing;
}
