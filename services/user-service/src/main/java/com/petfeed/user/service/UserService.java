package com.petfeed.user.service;

import com.petfeed.domain.dto.request.FollowRequest;
import com.petfeed.domain.dto.request.SignupRequest;
import com.petfeed.domain.dto.response.SignupResponse;
import com.petfeed.domain.dto.response.UserProfileResponse;

public interface UserService {

    SignupResponse signup(SignupRequest request);

    UserProfileResponse getProfile(String userKey);

    void follow(String targetUserKey, FollowRequest request);
}
