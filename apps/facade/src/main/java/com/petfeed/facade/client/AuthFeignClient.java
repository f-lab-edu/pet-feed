package com.petfeed.facade.client;

import com.petfeed.domain.dto.request.LoginRequest;
import com.petfeed.domain.dto.response.LoginResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", url = "${services.user-service.url}")
public interface AuthFeignClient {

    @PostMapping("/api/v1/auth/login")
    LoginResponse login(@RequestBody LoginRequest request);
}
