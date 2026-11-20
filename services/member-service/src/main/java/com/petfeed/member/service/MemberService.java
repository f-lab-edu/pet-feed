package com.petfeed.member.service;

import com.petfeed.contracts.dto.request.SignupRequest;
import com.petfeed.contracts.dto.request.LoginRequest;
import com.petfeed.contracts.dto.request.UpdateProfileRequest;
import com.petfeed.contracts.dto.response.LoginResponse;
import com.petfeed.contracts.dto.response.AuthorResponse;
import com.petfeed.contracts.dto.response.SignupResponse;
import com.petfeed.contracts.dto.response.MemberInfoResponse;

public interface MemberService {

    AuthorResponse getAuthor(String memberKey);

    SignupResponse signup(SignupRequest request);

    MemberInfoResponse getProfile(String memberKey);

    LoginResponse login(LoginRequest request);

    void updateProfile(String memberKey, String requesterKey, UpdateProfileRequest request);

    void delete(String memberKey, String requesterKey);
}
