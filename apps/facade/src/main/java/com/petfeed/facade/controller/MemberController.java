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

    @PostMapping("/members/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(201).body(memberFacade.signup(request));
    }

    @GetMapping("/members/{member-key}")
    public ResponseEntity<MemberProfileResponse> getProfile(
            @PathVariable("member-key") String memberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey) { // requesterKey 는 MDC에 넣을까 고민해볼필요
        return ResponseEntity.ok(memberFacade.getProfile(memberKey, requesterKey));
    }

    @GetMapping("/members/{member-key}/posts")
    public ResponseEntity<PostListResponse> getPostsByMember(
            @PathVariable("member-key") String memberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey,
            @RequestParam(defaultValue = "0") Long cursor) {

        return ResponseEntity.ok(memberFacade.getPostsByMember(memberKey, cursor));
    }

    @GetMapping("/members/{member-key}/followers")
    public ResponseEntity<MemberListResponse> getFollowers(
            @PathVariable("member-key") String memberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey,
            @RequestParam(defaultValue = "0") Long cursor) {

        return ResponseEntity.ok(followFacade.list(memberKey, requesterKey, cursor, true));
    }

    @GetMapping("/members/{member-key}/followings")
    public ResponseEntity<MemberListResponse> getFollowings(
            @PathVariable("member-key") String memberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey,
            @RequestParam(defaultValue = "0") Long cursor) {

        return ResponseEntity.ok(followFacade.list(memberKey, requesterKey, cursor, false));
    }

    @PutMapping("/members/{member-key}")
    public ResponseEntity<Void> updateProfile(
            @PathVariable("member-key") String memberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey,
            @Valid @RequestBody UpdateProfileRequest request) {

        memberFacade.updateProfile(memberKey, requesterKey, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/members/{member-key}")
    public ResponseEntity<Void> delete(
            @PathVariable("member-key") String memberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey) {
        memberFacade.delete(memberKey, requesterKey);
        return ResponseEntity.noContent().build();
    }

    // PATCH → POST/DELETE 분리
    // java.net.HttpURLConnection이 PATCH를 지원하지 않아 Feign 클라이언트 호환성 문제 발생.
    // 팔로우(생성)/언팔로우(삭제) 의미에도 POST/DELETE가 더 명확한 REST 시맨틱에 부합.
    @PostMapping("/following/{member-key}")
    public ResponseEntity<Void> follow(
            @PathVariable("member-key") String targetMemberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey) {

        followFacade.follow(requesterKey, targetMemberKey);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/following/{member-key}")
    public ResponseEntity<Void> unfollow(
            @PathVariable("member-key") String targetMemberKey,
            @RequestHeader(X_MEMBER_KEY) String requesterKey) {

        followFacade.unfollow(requesterKey, targetMemberKey);
        return ResponseEntity.ok().build();
    }
}
