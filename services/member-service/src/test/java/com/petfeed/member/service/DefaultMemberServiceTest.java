package com.petfeed.member.service;

import com.petfeed.contracts.dto.request.*;
import com.petfeed.infrastructure.jwt.JwtTokenProvider;
import com.petfeed.member.domain.Member;
import com.petfeed.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultMemberServiceTest {
    @Mock MemberRepository members;
    @Mock JwtTokenProvider tokens;
    BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);
    DefaultMemberService service;
    Member member;

    @BeforeEach
    void setUp() {
        service = new DefaultMemberService(members, passwords, tokens);
        member = Member.create("owner@example.com", passwords.encode("secret123!"), "01012345678", "owner");
    }

    @Test
    void signupNormalizesEmailAndHashesPassword() {
        when(members.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.signup(SignupRequest.of("OWNER@example.com", "secret123!", "01012345678", "owner"));
        var saved = org.mockito.ArgumentCaptor.forClass(Member.class);
        verify(members).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("owner@example.com");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("secret123!");
        assertThat(passwords.matches("secret123!", saved.getValue().getPasswordHash())).isTrue();
        assertThat(response.getMemberKey()).isEqualTo(saved.getValue().getMemberKey());
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void duplicateSignupIsRejected() {
        when(members.existsByEmail("owner@example.com")).thenReturn(true);
        assertStatus(() -> service.signup(SignupRequest.of("OWNER@example.com", "secret123!", "010", "owner")), HttpStatus.CONFLICT);
        verify(members, never()).saveAndFlush(any());
    }

    @Test
    void loginIssuesTokenForMatchingPassword() {
        when(members.findByEmail("owner@example.com")).thenReturn(Optional.of(member));
        when(tokens.generateToken(member.getMemberKey())).thenReturn("access-token");
        assertThat(service.login(LoginRequest.of("OWNER@example.com", "secret123!")).getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void wrongPasswordDoesNotIssueToken() {
        when(members.findByEmail("owner@example.com")).thenReturn(Optional.of(member));
        assertStatus(() -> service.login(LoginRequest.of("owner@example.com", "wrong")), HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(tokens);
    }

    @Test
    void unknownEmailCannotLogin() {
        assertStatus(() -> service.login(LoginRequest.of("missing@example.com", "secret123!")), HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(tokens);
    }

    @Test
    void profileReturnsPublicMemberInformation() {
        when(members.findById(member.getMemberKey())).thenReturn(Optional.of(member));
        assertThat(service.getProfile(member.getMemberKey()).getNickname()).isEqualTo("owner");
    }

    @Test
    void missingProfileIsNotFound() {
        assertStatus(() -> service.getProfile("missing"), HttpStatus.NOT_FOUND);
    }

    @Test
    void ownerCanUpdateEmailAndBio() {
        String key = member.getMemberKey();
        when(members.findById(key)).thenReturn(Optional.of(member));
        service.updateProfile(key, key, UpdateProfileRequest.of("new bio", "NEW@example.com"));
        assertThat(member.getEmail()).isEqualTo("new@example.com");
        assertThat(member.getBio()).isEqualTo("new bio");
        assertThat(passwords.matches("secret123!", member.getPasswordHash())).isTrue();
    }

    @Test
    void omittedUpdateFieldsArePreserved() {
        String key = member.getMemberKey();
        member.update(null, "old bio");
        when(members.findById(key)).thenReturn(Optional.of(member));
        service.updateProfile(key, key, UpdateProfileRequest.of(null, null));
        assertThat(member.getEmail()).isEqualTo("owner@example.com");
        assertThat(member.getBio()).isEqualTo("old bio");
    }

    @Test
    void duplicateEmailUpdateDoesNotChangeMember() {
        String key = member.getMemberKey();
        when(members.findById(key)).thenReturn(Optional.of(member));
        when(members.existsByEmailAndMemberKeyNot("taken@example.com", key)).thenReturn(true);
        assertStatus(() -> service.updateProfile(key, key, UpdateProfileRequest.of("changed", "taken@example.com")), HttpStatus.CONFLICT);
        assertThat(member.getEmail()).isEqualTo("owner@example.com");
        assertThat(member.getBio()).isNull();
    }

    @Test
    void anotherMemberCannotUpdateOrDelete() {
        assertStatus(() -> service.updateProfile(member.getMemberKey(), "other", UpdateProfileRequest.of("changed", null)), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.delete(member.getMemberKey(), "other"), HttpStatus.FORBIDDEN);
        verifyNoInteractions(members);
    }

    @Test
    void ownerCanDeleteMember() {
        String key = member.getMemberKey();
        when(members.findById(key)).thenReturn(Optional.of(member));
        service.delete(key, key);
        assertThat(member.getStatus()).isEqualTo(com.petfeed.member.domain.MemberStatus.WITHDRAWN);
        verify(members, never()).delete(any());
    }

    @Test
    void withdrawnMemberCannotLoginReadProfileOrUpdate() {
        member.withdraw();
        String key = member.getMemberKey();
        when(members.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
        when(members.findById(key)).thenReturn(Optional.of(member));
        assertStatus(() -> service.login(LoginRequest.of(member.getEmail(), "secret123!")), HttpStatus.UNAUTHORIZED);
        assertStatus(() -> service.getProfile(key), HttpStatus.NOT_FOUND);
        assertStatus(() -> service.updateProfile(key, key, UpdateProfileRequest.of("changed", null)), HttpStatus.NOT_FOUND);
        verifyNoInteractions(tokens);
    }

    @Test
    void authorLookupDistinguishesActiveWithdrawnAndMissingMembers() {
        String key = member.getMemberKey();
        when(members.findById(key)).thenReturn(Optional.of(member));
        assertThat(service.getAuthor(key).getStatus()).isEqualTo(com.petfeed.contracts.dto.response.AuthorStatus.ACTIVE);
        assertThat(service.getAuthor(key).getDisplayName()).isEqualTo("owner");
        member.withdraw();
        assertThat(service.getAuthor(key).getStatus()).isEqualTo(com.petfeed.contracts.dto.response.AuthorStatus.WITHDRAWN);
        assertThat(service.getAuthor(key).getDisplayName()).isEqualTo("(탈퇴한 회원)");
        assertThat(member.getNickname()).isEqualTo("owner");
        assertStatus(() -> service.getAuthor("missing"), HttpStatus.NOT_FOUND);
    }

    @Test
    void missingMemberCannotBeUpdatedOrDeleted() {
        assertStatus(() -> service.updateProfile("missing", "missing", UpdateProfileRequest.of("bio", null)), HttpStatus.NOT_FOUND);
        assertStatus(() -> service.delete("missing", "missing"), HttpStatus.NOT_FOUND);
        verify(members, never()).delete(any());
    }

    private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, HttpStatus status) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
    }
}
