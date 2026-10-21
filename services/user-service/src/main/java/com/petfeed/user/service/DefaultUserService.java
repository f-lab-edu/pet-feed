package com.petfeed.user.service;

import com.petfeed.domain.dto.request.*;
import com.petfeed.domain.dto.response.*;
import com.petfeed.infrastructure.jwt.JwtTokenProvider;
import com.petfeed.user.domain.Member;
import com.petfeed.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultUserService implements UserService {
    private final MemberRepository members;
    private final PasswordEncoder passwords;
    private final JwtTokenProvider tokens;
    @Value("${jwt.expiration}")
    private long expirationMs;

    @Override
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = normalize(request.getEmail());
        if (members.existsByEmail(email)) throw error(HttpStatus.CONFLICT, "Email already registered");
        Member member = members.saveAndFlush(new Member(email, passwords.encode(request.getPassword()),
                request.getPhone(), request.getNickname()));
        return new SignupResponse(member.getUserKey(), member.getCreatedAt());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Member member = members.findByEmail(normalize(request.getEmail()))
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwords.matches(request.getPassword(), member.getPasswordHash()))
            throw error(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        // Refresh-token lifecycle is outside the member CRUD scope.
        return new LoginResponse(tokens.generateToken(member.getUserKey()), null,
                Long.toString(expirationMs), "Bearer");
    }

    @Override
    public UserProfileResponse getProfile(String userKey) {
        Member member = requireMember(userKey);
        return new UserProfileResponse(member.getNickname(), null, member.getBio(), 0, 0, false, 0);
    }

    @Override
    @Transactional
    public void updateProfile(String userKey, String requesterKey, UpdateProfileRequest request) {
        requireOwner(userKey, requesterKey);
        Member member = requireMember(userKey);
        String email = request.getEmail() == null ? null : normalize(request.getEmail());
        if (email != null && members.existsByEmailAndUserKeyNot(email, userKey))
            throw error(HttpStatus.CONFLICT, "Email already registered");
        member.update(email, request.getBio());
        members.flush();
    }

    @Override
    @Transactional
    public void delete(String userKey, String requesterKey) {
        requireOwner(userKey, requesterKey);
        members.delete(requireMember(userKey));
    }

    private Member requireMember(String userKey) {
        return members.findById(userKey).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Member not found"));
    }

    private void requireOwner(String userKey, String requesterKey) {
        if (!userKey.equals(requesterKey)) throw error(HttpStatus.FORBIDDEN, "Only the owner can change this member");
    }

    private String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
