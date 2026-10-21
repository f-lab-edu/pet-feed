package com.petfeed.user.controller;

import com.petfeed.domain.dto.request.SignupRequest;
import com.petfeed.domain.dto.response.SignupResponse;
import com.petfeed.domain.dto.response.UserProfileResponse;
import com.petfeed.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
}
