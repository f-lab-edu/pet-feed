package com.petfeed.facade.client;

import com.petfeed.contracts.dto.request.SignupRequest;
import com.petfeed.contracts.dto.request.UpdateProfileRequest;
import com.petfeed.contracts.dto.response.SignupResponse;
import com.petfeed.contracts.dto.response.AuthorResponse;
import com.petfeed.contracts.dto.response.MemberListResponse;
import com.petfeed.contracts.dto.response.MemberInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import static com.petfeed.contracts.http.MyHttpHeaders.X_MEMBER_KEY;

@FeignClient(name = "member-service", url = "${services.member-service.url}")
public interface MemberFeignClient {

    @GetMapping("/internal/v1/members/{member-key}/author")
    AuthorResponse getAuthor(@PathVariable("member-key") String memberKey);

    @PostMapping("/api/v1/members/signup")
    SignupResponse signup(@RequestBody SignupRequest request);

    @GetMapping("/api/v1/members/{member-key}")
    MemberInfoResponse getProfile(@PathVariable("member-key") String memberKey);

    @PutMapping("/api/v1/members/{member-key}")
    void updateProfile(@PathVariable("member-key") String memberKey,
                       @RequestHeader(X_MEMBER_KEY) String requesterKey,
                       @RequestBody UpdateProfileRequest request);

    @DeleteMapping("/api/v1/members/{member-key}")
    void delete(@PathVariable("member-key") String memberKey,
                @RequestHeader(X_MEMBER_KEY) String requesterKey);

}
