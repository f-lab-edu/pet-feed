package com.petfeed.gateway.controller;

import com.petfeed.domain.dto.request.LoginRequest;
import com.petfeed.domain.dto.request.SignupRequest;
import com.petfeed.domain.dto.response.LoginResponse;
import com.petfeed.domain.dto.response.SignupResponse;
import com.petfeed.gateway.client.FacadeClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final FacadeClient facadeClient;

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(facadeClient.login(request));
    }

    @PostMapping("/users/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(facadeClient.signup(request));
    }
}
