package com.petfeed.facade.controller;

import com.petfeed.domain.dto.request.FollowRequest;
import com.petfeed.domain.dto.request.SignupRequest;
import com.petfeed.domain.dto.request.UpdateProfileRequest;
import com.petfeed.domain.dto.response.*;
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

    @GetMapping("/users/{user-key}/posts")
    public ResponseEntity<PostListResponse> getPostsByUser(
            @PathVariable("user-key") String userKey,
            @RequestHeader("X-User-Key") String requesterKey,
            @RequestParam(defaultValue = "0") Long cursor) {
        return ResponseEntity.ok(userFacade.getPostsByUser(userKey, cursor));
    }

    @GetMapping("/users/{user-key}/followers")
    public ResponseEntity<UserListResponse> getFollowers(
            @PathVariable("user-key") String userKey,
            @RequestHeader("X-User-Key") String requesterKey,
            @RequestParam(defaultValue = "0") Long cursor) {
        return ResponseEntity.ok(userFacade.getFollowers(userKey, cursor));
    }

    @GetMapping("/users/{user-key}/followings")
    public ResponseEntity<UserListResponse> getFollowings(
            @PathVariable("user-key") String userKey,
            @RequestHeader("X-User-Key") String requesterKey,
            @RequestParam(defaultValue = "0") Long cursor) {
        return ResponseEntity.ok(userFacade.getFollowings(userKey, cursor));
    }

    @PutMapping("/users/{user-key}")
    public ResponseEntity<Void> updateProfile(
            @PathVariable("user-key") String userKey,
            @RequestHeader("X-User-Key") String requesterKey,
            @RequestBody UpdateProfileRequest request) {
        userFacade.updateProfile(userKey, request);
        return ResponseEntity.noContent().build();
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
