package com.petfeed.facade;

import com.petfeed.domain.dto.request.LoginRequest;
import com.petfeed.domain.dto.response.LoginResponse;
import com.petfeed.facade.client.AuthFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFacade {

    private final AuthFeignClient authFeignClient;

    public LoginResponse login(LoginRequest request) {
        return authFeignClient.login(request);
    }
}
