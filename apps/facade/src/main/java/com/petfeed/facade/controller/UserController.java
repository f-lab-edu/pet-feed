package com.petfeed.facade.controller;

import com.petfeed.domain.dto.request.FollowRequest;
import com.petfeed.domain.dto.request.SignupRequest;
import com.petfeed.domain.dto.response.SignupResponse;
import com.petfeed.domain.dto.response.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserFacade userFacade;

    @PostMapping("/users/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(userFacade.signup(request));
    }

    @GetMapping("/users/{user-key}")
    public ResponseEntity<UserProfileResponse> getProfile(
            @PathVariable("user-key") String userKey,
            @RequestHeader("X-User-Key") String requesterKey) { // requesterKey 는 MDC에 넣을까 고민해볼필요
        return ResponseEntity.ok(userFacade.getProfile(userKey));
    }

    @PatchMapping("/following/{user-key}")
    public ResponseEntity<Void> follow(
            @PathVariable("user-key") String targetUserKey,
            @RequestHeader("X-User-Key") String requesterKey,
            @RequestBody FollowRequest request) {
        userFacade.follow(targetUserKey, request);
        return ResponseEntity.ok().build();
    }
}
