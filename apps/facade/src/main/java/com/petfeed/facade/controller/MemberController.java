package com.petfeed.facade.controller;

import com.petfeed.contracts.dto.request.SignupRequest;
import com.petfeed.contracts.dto.request.UpdateProfileRequest;
import com.petfeed.contracts.dto.response.PostListResponse;
import com.petfeed.contracts.dto.response.SignupResponse;
import com.petfeed.contracts.dto.response.MemberListResponse;
import com.petfeed.contracts.dto.response.MemberProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.petfeed.contracts.http.MyHttpHeaders.X_MEMBER_KEY;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberController {

    private final MemberFacade memberFacade;
    private final FollowFacade followFacade;

    @PostMapping("/users/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(201).body(memberFacade.signup(request));
    }

    @GetMapping("/users/{user-key}")
    public ResponseEntity<MemberProfileResponse> getProfile(
            @PathVariable("user-key") String memberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey) { // requesterKey 는 MDC에 넣을까 고민해볼필요
        return ResponseEntity.ok(followFacade.profile(memberKey, requesterKey, memberFacade.getProfile(memberKey)));
    }

    @GetMapping("/users/{user-key}/posts")
    public ResponseEntity<PostListResponse> getPostsByMember(
            @PathVariable("user-key") String memberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey,
            @RequestParam(defaultValue = "0") Long cursor) {

        return ResponseEntity.ok(memberFacade.getPostsByMember(memberKey, cursor));
    }

    @GetMapping("/users/{user-key}/followers")
    public ResponseEntity<MemberListResponse> getFollowers(
            @PathVariable("user-key") String memberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey,
            @RequestParam(defaultValue = "0") Long cursor) {

        return ResponseEntity.ok(followFacade.list(memberKey, requesterKey, cursor, true));
    }

    @GetMapping("/users/{user-key}/followings")
    public ResponseEntity<MemberListResponse> getFollowings(
            @PathVariable("user-key") String memberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey,
            @RequestParam(defaultValue = "0") Long cursor) {

        return ResponseEntity.ok(followFacade.list(memberKey, requesterKey, cursor, false));
    }

    @PutMapping("/users/{user-key}")
    public ResponseEntity<Void> updateProfile(
            @PathVariable("user-key") String memberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey,
            @Valid @RequestBody UpdateProfileRequest request) {

        memberFacade.updateProfile(memberKey, requesterKey, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{user-key}")
    public ResponseEntity<Void> delete(
            @PathVariable("user-key") String memberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey) {
        memberFacade.delete(memberKey, requesterKey);
        return ResponseEntity.noContent().build();
    }

    // PATCH → POST/DELETE 분리
    // java.net.HttpURLConnection이 PATCH를 지원하지 않아 Feign 클라이언트 호환성 문제 발생.
    // 팔로우(생성)/언팔로우(삭제) 의미에도 POST/DELETE가 더 명확한 REST 시맨틱에 부합.
    @PostMapping("/following/{user-key}")
    public ResponseEntity<Void> follow(
            @PathVariable("user-key") String targetMemberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey) {

        followFacade.follow(requesterKey, targetMemberKey);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/following/{user-key}")
    public ResponseEntity<Void> unfollow(
            @PathVariable("user-key") String targetMemberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey) {

        followFacade.unfollow(requesterKey, targetMemberKey);
        return ResponseEntity.ok().build();
    }
}
