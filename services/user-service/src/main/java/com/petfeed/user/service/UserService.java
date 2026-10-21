package com.petfeed.user.service;

import com.petfeed.domain.dto.request.SignupRequest;
import com.petfeed.domain.dto.request.LoginRequest;
import com.petfeed.domain.dto.request.UpdateProfileRequest;
import com.petfeed.domain.dto.response.LoginResponse;
import com.petfeed.domain.dto.response.SignupResponse;
import com.petfeed.domain.dto.response.UserProfileResponse;

public interface UserService {

    SignupResponse signup(SignupRequest request);

    UserProfileResponse getProfile(String userKey);

    LoginResponse login(LoginRequest request);

    void updateProfile(String userKey, String requesterKey, UpdateProfileRequest request);

    void delete(String userKey, String requesterKey);
}
