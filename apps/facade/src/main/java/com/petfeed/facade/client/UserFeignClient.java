package com.petfeed.facade.client;

import com.petfeed.domain.dto.request.FollowRequest;
import com.petfeed.domain.dto.request.SignupRequest;
import com.petfeed.domain.dto.response.SignupResponse;
import com.petfeed.domain.dto.response.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserFeignClient {

    @PostMapping("/api/v1/users/signup")
    SignupResponse signup(@RequestBody SignupRequest request);

    @GetMapping("/api/v1/users/{user-key}")
    UserProfileResponse getProfile(@PathVariable("user-key") String userKey);

    @PatchMapping("/api/v1/following/{user-key}")
    void follow(@PathVariable("user-key") String targetUserKey, @RequestBody FollowRequest request);
}
