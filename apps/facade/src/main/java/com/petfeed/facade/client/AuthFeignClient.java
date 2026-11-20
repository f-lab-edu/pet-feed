package com.petfeed.facade.client;

import com.petfeed.contracts.http.ApiPaths;

import com.petfeed.contracts.dto.request.LoginRequest;
import com.petfeed.contracts.dto.response.LoginResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", url = "${services.member-service.url}")
public interface AuthFeignClient {

    @PostMapping(ApiPaths.API_V1 + "/auth/login")
    LoginResponse login(@RequestBody LoginRequest request);
}
