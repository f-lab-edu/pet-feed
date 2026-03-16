package com.petfeed.facade;

import com.petfeed.domain.dto.request.FollowRequest;
import com.petfeed.domain.dto.request.SignupRequest;
import com.petfeed.domain.dto.response.SignupResponse;
import com.petfeed.domain.dto.response.UserProfileResponse;
import com.petfeed.facade.client.PostFeignClient;
import com.petfeed.facade.client.UserFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserFeignClient userFeignClient;
    private final PostFeignClient postFeignClient;

    public SignupResponse signup(SignupRequest request) {
        return userFeignClient.signup(request);
    }

    public UserProfileResponse getProfile(String userKey) {
        UserProfileResponse profile = userFeignClient.getProfile(userKey);
        long postCount = postFeignClient.getPostCount(userKey);
        return new UserProfileResponse(
                profile.getNickname(),
                profile.getProfileImageUrl(),
                profile.getBio(),
                profile.getFollowerCount(),
                profile.getFollowingCount(),
                profile.isFollowing(),
                postCount
        );
    }

    public void follow(String targetUserKey, FollowRequest request) {
        userFeignClient.follow(targetUserKey, request);
    }
}
