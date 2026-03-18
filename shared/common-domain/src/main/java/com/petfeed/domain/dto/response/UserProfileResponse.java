package com.petfeed.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private String nickname;
    private String profileImageUrl;
    private String bio;
    private int followerCount;
    private int followingCount;
    private boolean isFollowing;
    private long postCount;
}
