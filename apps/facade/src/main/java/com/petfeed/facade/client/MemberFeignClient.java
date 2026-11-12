package com.petfeed.facade.client;

import com.petfeed.contracts.dto.request.SignupRequest;
import com.petfeed.contracts.dto.request.UpdateProfileRequest;
import com.petfeed.contracts.dto.response.SignupResponse;
import com.petfeed.contracts.dto.response.AuthorResponse;
import com.petfeed.contracts.dto.response.MemberListResponse;
import com.petfeed.contracts.dto.response.MemberProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import static com.petfeed.contracts.http.MyHttpHeaders.X_MEMBER_KEY;

@FeignClient(name = "member-service", url = "${services.member-service.url}")
public interface MemberFeignClient {

    @GetMapping("/internal/v1/users/{user-key}/author")
    AuthorResponse getAuthor(@PathVariable("user-key") String memberKey);

    @PostMapping("/api/v1/users/signup")
    SignupResponse signup(@RequestBody SignupRequest request);

    @GetMapping("/api/v1/users/{user-key}")
    MemberProfileResponse getProfile(@PathVariable("user-key") String memberKey);

    @PutMapping("/api/v1/users/{user-key}")
    void updateProfile(@PathVariable("user-key") String memberKey,
                       @RequestHeader(X_MEMBER_KEY) String requesterKey,
                       @RequestBody UpdateProfileRequest request);

    @DeleteMapping("/api/v1/users/{user-key}")
    void delete(@PathVariable("user-key") String memberKey,
                @RequestHeader(X_MEMBER_KEY) String requesterKey);

}
