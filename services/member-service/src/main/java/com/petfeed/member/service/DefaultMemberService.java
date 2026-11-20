package com.petfeed.member.service;

import com.petfeed.contracts.dto.request.*;
import com.petfeed.contracts.dto.response.*;
import com.petfeed.infrastructure.jwt.JwtTokenProvider;
import com.petfeed.member.domain.Member;
import com.petfeed.member.domain.MemberStatus;
import com.petfeed.member.repository.MemberRepository;
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
public class DefaultMemberService implements MemberService {
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
        Member member = members.saveAndFlush(Member.create(email, passwords.encode(request.getPassword()),
                request.getPhone(), request.getNickname()));
        return SignupResponse.of(member.getMemberKey(), member.getCreatedAt());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Member member = members.findByEmail(normalize(request.getEmail()))
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (member.getStatus() == MemberStatus.WITHDRAWN
                || !passwords.matches(request.getPassword(), member.getPasswordHash()))
            throw error(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        // Refresh-token lifecycle is outside the member CRUD scope.
        return LoginResponse.of(tokens.generateToken(member.getMemberKey()), null,
                Long.toString(expirationMs), "Bearer");
    }

    @Override
    public MemberInfoResponse getProfile(String memberKey) {
        Member member = requireMember(memberKey);
        return MemberInfoResponse.of(member.getNickname(), null, member.getBio());
    }

    @Override
    @Transactional
    public void updateProfile(String memberKey, String requesterKey, UpdateProfileRequest request) {
        requireOwner(memberKey, requesterKey);
        Member member = requireMember(memberKey);
        String email = request.getEmail() == null ? null : normalize(request.getEmail());
        if (email != null && members.existsByEmailAndMemberKeyNot(email, memberKey))
            throw error(HttpStatus.CONFLICT, "Email already registered");
        member.update(email, request.getBio());
        members.flush();
    }

    @Override
    @Transactional
    public void delete(String memberKey, String requesterKey) {
        requireOwner(memberKey, requesterKey);
        requireMember(memberKey).withdraw();
    }

    @Override
    public AuthorResponse getAuthor(String memberKey) {
        Member member = members.findById(memberKey)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Member not found"));
        AuthorStatus status = switch (member.getStatus()) {
            case ACTIVE -> AuthorStatus.ACTIVE;
            case WITHDRAWN -> AuthorStatus.WITHDRAWN;
        };
        return AuthorResponse.of(status, member.getStatus() == MemberStatus.WITHDRAWN
                ? "(탈퇴한 회원)" : member.getNickname());
    }

    private Member requireMember(String memberKey) {
        return members.findById(memberKey).filter(member -> member.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Member not found"));
    }

    private void requireOwner(String memberKey, String requesterKey) {
        if (!memberKey.equals(requesterKey)) throw error(HttpStatus.FORBIDDEN, "Only the owner can change this member");
    }

    private String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
