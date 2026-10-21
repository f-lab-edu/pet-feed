package com.petfeed.user.controller;

import com.petfeed.domain.dto.request.SignupRequest;
import com.petfeed.domain.dto.request.LoginRequest;
import com.petfeed.domain.dto.request.UpdateProfileRequest;
import com.petfeed.domain.dto.response.LoginResponse;
import com.petfeed.domain.dto.response.SignupResponse;
import com.petfeed.domain.dto.response.UserProfileResponse;
import com.petfeed.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.petfeed.domain.http.MyHttpHeaders.X_USER_KEY;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/api/v1/users/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@RequestBody @Valid SignupRequest request) {
        return userService.signup(request);
    }

    @GetMapping("/api/v1/users/{user-key}")
    public UserProfileResponse getProfile(@PathVariable("user-key") String userKey) {
        return userService.getProfile(userKey);
    }

    @PostMapping("/api/v1/auth/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return userService.login(request);
    }

    @PutMapping("/api/v1/users/{user-key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable("user-key") String userKey,
                       @RequestHeader(X_USER_KEY) String requesterKey,
                       @RequestBody @Valid UpdateProfileRequest request) {
        userService.updateProfile(userKey, requesterKey, request);
    }

    @DeleteMapping("/api/v1/users/{user-key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("user-key") String userKey,
                       @RequestHeader(X_USER_KEY) String requesterKey) {
        userService.delete(userKey, requesterKey);
    }
}
