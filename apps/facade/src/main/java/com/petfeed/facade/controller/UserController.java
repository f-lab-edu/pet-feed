package com.petfeed.facade.controller;

import com.petfeed.domain.dto.request.SignupRequest;
import com.petfeed.domain.dto.request.UpdateProfileRequest;
import com.petfeed.domain.dto.response.PostListResponse;
import com.petfeed.domain.dto.response.SignupResponse;
import com.petfeed.domain.dto.response.UserListResponse;
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

    // PATCH → POST/DELETE 분리
    // java.net.HttpURLConnection이 PATCH를 지원하지 않아 Feign 클라이언트 호환성 문제 발생.
    // 팔로우(생성)/언팔로우(삭제) 의미에도 POST/DELETE가 더 명확한 REST 시맨틱에 부합.
    @PostMapping("/following/{user-key}")
    public ResponseEntity<Void> follow(
            @PathVariable("user-key") String targetUserKey,
            @RequestHeader("X-User-Key") String requesterKey) {
        userFacade.follow(targetUserKey);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/following/{user-key}")
    public ResponseEntity<Void> unfollow(
            @PathVariable("user-key") String targetUserKey,
            @RequestHeader("X-User-Key") String requesterKey) {
        userFacade.unfollow(targetUserKey);
        return ResponseEntity.ok().build();
    }
}
