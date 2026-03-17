package com.petfeed.gateway.controller;

import com.petfeed.domain.dto.request.FollowRequest;
import com.petfeed.domain.dto.response.UserProfileResponse;
import com.petfeed.gateway.client.FacadeClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final FacadeClient facadeClient;

    @GetMapping("/users/{user-key}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable("user-key") String userKey) {
        return ResponseEntity.ok(facadeClient.getProfile(userKey));
    }

    @PatchMapping("/following/{user-key}")
    public ResponseEntity<Void> follow(@PathVariable("user-key") String targetUserKey,
                                       @Valid @RequestBody FollowRequest request) {
        facadeClient.follow(targetUserKey, request);
        return ResponseEntity.ok().build();
    }
}
