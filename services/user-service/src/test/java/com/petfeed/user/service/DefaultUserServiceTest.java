package com.petfeed.user.service;

import com.petfeed.domain.dto.request.*;
import com.petfeed.infrastructure.jwt.JwtTokenProvider;
import com.petfeed.user.domain.Member;
import com.petfeed.user.repository.MemberRepository;
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
class DefaultUserServiceTest {
    @Mock MemberRepository members;
    @Mock JwtTokenProvider tokens;
    BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);
    DefaultUserService service;
    Member member;

    @BeforeEach
    void setUp() {
        service = new DefaultUserService(members, passwords, tokens);
        member = new Member("owner@example.com", passwords.encode("secret123!"), "01012345678", "owner");
    }

    @Test
    void signupNormalizesEmailAndHashesPassword() {
        when(members.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.signup(new SignupRequest("OWNER@example.com", "secret123!", "01012345678", "owner"));
        var saved = org.mockito.ArgumentCaptor.forClass(Member.class);
        verify(members).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("owner@example.com");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("secret123!");
        assertThat(passwords.matches("secret123!", saved.getValue().getPasswordHash())).isTrue();
        assertThat(response.getUserKey()).isEqualTo(saved.getValue().getUserKey());
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void duplicateSignupIsRejected() {
        when(members.existsByEmail("owner@example.com")).thenReturn(true);
        assertStatus(() -> service.signup(new SignupRequest("OWNER@example.com", "secret123!", "010", "owner")), HttpStatus.CONFLICT);
        verify(members, never()).saveAndFlush(any());
    }

    @Test
    void loginIssuesTokenForMatchingPassword() {
        when(members.findByEmail("owner@example.com")).thenReturn(Optional.of(member));
        when(tokens.generateToken(member.getUserKey())).thenReturn("access-token");
        assertThat(service.login(new LoginRequest("OWNER@example.com", "secret123!")).getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void wrongPasswordDoesNotIssueToken() {
        when(members.findByEmail("owner@example.com")).thenReturn(Optional.of(member));
        assertStatus(() -> service.login(new LoginRequest("owner@example.com", "wrong")), HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(tokens);
    }

    @Test
    void unknownEmailCannotLogin() {
        assertStatus(() -> service.login(new LoginRequest("missing@example.com", "secret123!")), HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(tokens);
    }

    @Test
    void profileReturnsPublicMemberInformation() {
        when(members.findById(member.getUserKey())).thenReturn(Optional.of(member));
        assertThat(service.getProfile(member.getUserKey()).getNickname()).isEqualTo("owner");
    }

    @Test
    void missingProfileIsNotFound() {
        assertStatus(() -> service.getProfile("missing"), HttpStatus.NOT_FOUND);
    }

    @Test
    void ownerCanUpdateEmailAndBio() {
        String key = member.getUserKey();
        when(members.findById(key)).thenReturn(Optional.of(member));
        service.updateProfile(key, key, new UpdateProfileRequest("new bio", "NEW@example.com"));
        assertThat(member.getEmail()).isEqualTo("new@example.com");
        assertThat(member.getBio()).isEqualTo("new bio");
        assertThat(passwords.matches("secret123!", member.getPasswordHash())).isTrue();
    }

    @Test
    void omittedUpdateFieldsArePreserved() {
        String key = member.getUserKey();
        member.update(null, "old bio");
        when(members.findById(key)).thenReturn(Optional.of(member));
        service.updateProfile(key, key, new UpdateProfileRequest(null, null));
        assertThat(member.getEmail()).isEqualTo("owner@example.com");
        assertThat(member.getBio()).isEqualTo("old bio");
    }

    @Test
    void duplicateEmailUpdateDoesNotChangeMember() {
        String key = member.getUserKey();
        when(members.findById(key)).thenReturn(Optional.of(member));
        when(members.existsByEmailAndUserKeyNot("taken@example.com", key)).thenReturn(true);
        assertStatus(() -> service.updateProfile(key, key, new UpdateProfileRequest("changed", "taken@example.com")), HttpStatus.CONFLICT);
        assertThat(member.getEmail()).isEqualTo("owner@example.com");
        assertThat(member.getBio()).isNull();
    }

    @Test
    void anotherMemberCannotUpdateOrDelete() {
        assertStatus(() -> service.updateProfile(member.getUserKey(), "other", new UpdateProfileRequest("changed", null)), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.delete(member.getUserKey(), "other"), HttpStatus.FORBIDDEN);
        verifyNoInteractions(members);
    }

    @Test
    void ownerCanDeleteMember() {
        String key = member.getUserKey();
        when(members.findById(key)).thenReturn(Optional.of(member));
        service.delete(key, key);
        verify(members).delete(member);
    }

    @Test
    void missingMemberCannotBeUpdatedOrDeleted() {
        assertStatus(() -> service.updateProfile("missing", "missing", new UpdateProfileRequest("bio", null)), HttpStatus.NOT_FOUND);
        assertStatus(() -> service.delete("missing", "missing"), HttpStatus.NOT_FOUND);
        verify(members, never()).delete(any());
    }

    private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, HttpStatus status) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
    }
}
