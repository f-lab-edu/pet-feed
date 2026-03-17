package com.petfeed.gateway.client;

import com.petfeed.domain.dto.request.FollowRequest;
import com.petfeed.domain.dto.request.LoginRequest;
import com.petfeed.domain.dto.request.SignupRequest;
import com.petfeed.domain.dto.response.LoginResponse;
import com.petfeed.domain.dto.response.SignupResponse;
import com.petfeed.domain.dto.response.UserProfileResponse;
import com.petfeed.facade.controller.AuthFacade;
import com.petfeed.facade.controller.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FacadeClient {

    private final AuthFacade authFacade;
    private final UserFacade userFacade;

    public LoginResponse login(LoginRequest request) {
        return authFacade.login(request);
    }

    public SignupResponse signup(SignupRequest request) {
        return userFacade.signup(request);
    }

    public UserProfileResponse getProfile(String targetUserKey) {
        return userFacade.getProfile(targetUserKey);
    }

    public void follow(String targetUserKey, FollowRequest request) {
        userFacade.follow(targetUserKey, request);
    }

    /**
     * 인가 처리 이후 SecurityContext에서 현재 인증된 사용자의 키를 반환합니다.
     * JwtAuthenticationFilter에서 설정된 principal을 동적으로 조회합니다.
     */
    public String getCurrentUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다.");
        }
        return (String) authentication.getPrincipal();
    }
}
