package com.petfeed.member.controller;

import com.petfeed.contracts.dto.request.SignupRequest;
import com.petfeed.contracts.dto.request.LoginRequest;
import com.petfeed.contracts.dto.request.UpdateProfileRequest;
import com.petfeed.contracts.dto.response.LoginResponse;
import com.petfeed.contracts.dto.response.AuthorResponse;
import com.petfeed.contracts.dto.response.SignupResponse;
import com.petfeed.contracts.dto.response.MemberInfoResponse;
import com.petfeed.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.petfeed.contracts.http.MyHttpHeaders.X_MEMBER_KEY;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/internal/v1/members/{member-key}/author")
    public AuthorResponse getAuthor(@PathVariable("member-key") String memberKey) {
        return memberService.getAuthor(memberKey);
    }

    @PostMapping("/api/v1/members/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@RequestBody @Valid SignupRequest request) {
        return memberService.signup(request);
    }

    @GetMapping("/api/v1/members/{member-key}")
    public MemberInfoResponse getProfile(@PathVariable("member-key") String memberKey) {
        return memberService.getProfile(memberKey);
    }

    @PostMapping("/api/v1/auth/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return memberService.login(request);
    }

    @PutMapping("/api/v1/members/{member-key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable("member-key") String memberKey,
                       @RequestHeader(X_MEMBER_KEY) String requesterKey,
                       @RequestBody @Valid UpdateProfileRequest request) {
        memberService.updateProfile(memberKey, requesterKey, request);
    }

    @DeleteMapping("/api/v1/members/{member-key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("member-key") String memberKey,
                       @RequestHeader(X_MEMBER_KEY) String requesterKey) {
        memberService.delete(memberKey, requesterKey);
    }
}
