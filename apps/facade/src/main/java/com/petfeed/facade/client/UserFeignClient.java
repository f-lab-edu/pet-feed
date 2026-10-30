package com.petfeed.facade.client;

import com.petfeed.domain.dto.request.SignupRequest;
import com.petfeed.domain.dto.request.UpdateProfileRequest;
import com.petfeed.domain.dto.response.SignupResponse;
import com.petfeed.domain.dto.response.AuthorResponse;
import com.petfeed.domain.dto.response.UserListResponse;
import com.petfeed.domain.dto.response.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import static com.petfeed.domain.http.MyHttpHeaders.X_USER_KEY;

@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserFeignClient {

    @GetMapping("/internal/v1/users/{user-key}/author")
    AuthorResponse getAuthor(@PathVariable("user-key") String userKey);

    @PostMapping("/api/v1/users/signup")
    SignupResponse signup(@RequestBody SignupRequest request);

    @GetMapping("/api/v1/users/{user-key}")
    UserProfileResponse getProfile(@PathVariable("user-key") String userKey);

    @PutMapping("/api/v1/users/{user-key}")
    void updateProfile(@PathVariable("user-key") String userKey,
                       @RequestHeader(X_USER_KEY) String requesterKey,
                       @RequestBody UpdateProfileRequest request);

    @DeleteMapping("/api/v1/users/{user-key}")
    void delete(@PathVariable("user-key") String userKey,
                @RequestHeader(X_USER_KEY) String requesterKey);

}
