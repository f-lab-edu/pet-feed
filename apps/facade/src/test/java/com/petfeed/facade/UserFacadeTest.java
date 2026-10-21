package com.petfeed.facade;

import com.petfeed.domain.dto.response.*;
import com.petfeed.facade.client.PostFeignClient;
import com.petfeed.facade.client.UserFeignClient;
import com.petfeed.facade.controller.UserFacade;
import com.petfeed.domain.dto.request.UpdateProfileRequest;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserFacadeTest {
    @Mock UserFeignClient users;
    @Mock PostFeignClient posts;
    @InjectMocks UserFacade facade;

    @Test
    @DisplayName("프로필과 게시물 수를 조합하고 원본 프로필은 변경하지 않는다")
    void profileCombinesMemberInformationAndPostCount() {
        var profile = UserProfileResponse.of("owner", "image", "bio", 5, 7, true, 0);
        when(users.getProfile("owner")).thenReturn(profile);
        when(posts.getPostCount("owner")).thenReturn(12L);

        var response = facade.getProfile("owner");

        assertThat(response).usingRecursiveComparison().isEqualTo(
                UserProfileResponse.of("owner", "image", "bio", 5, 7, true, 12));
        assertThat(profile.getPostCount()).isZero();
        verify(users).getProfile("owner");
        verify(posts).getPostCount("owner");
        verifyNoMoreInteractions(users, posts);
    }

    @Test
    @DisplayName("프로필 조회 실패 시 게시물 수 조회 없이 실패를 전파한다")
    void profileLookupFailureStopsOrchestration() {
        var failure = mock(FeignException.NotFound.class);
        when(users.getProfile("missing")).thenThrow(failure);

        assertThatThrownBy(() -> facade.getProfile("missing")).isSameAs(failure);
        verifyNoInteractions(posts);
    }

    @Test
    @DisplayName("필수 게시물 수 조회 실패 시 불완전한 프로필을 반환하지 않는다")
    void postCountFailureIsPropagated() {
        when(users.getProfile("owner")).thenReturn(
                UserProfileResponse.of("owner", null, null, 0, 0, false, 0));
        var failure = mock(FeignException.ServiceUnavailable.class);
        when(posts.getPostCount("owner")).thenThrow(failure);

        assertThatThrownBy(() -> facade.getProfile("owner")).isSameAs(failure);
    }

    @Test
    @DisplayName("정상 작성자 정보를 목록 전체에 조합하고 커서를 전달한다")
    void activeAuthorIsAttachedToEveryPost() {
        var author = AuthorResponse.of(MemberStatus.ACTIVE, "owner");
        when(users.getAuthor("owner")).thenReturn(author);
        when(posts.getPostsByUser("owner", 10L)).thenReturn(PostListResponse.of(List.of(
                PostSummary.of(1L, "first", 2, 3, null, null, null),
                PostSummary.of(2L, "second", 4, 5, null, null, null)), 20L, true));

        var response = facade.getPostsByUser("owner", 10L);

        assertThat(response.getPosts()).extracting(PostSummary::getPostId).containsExactly(1L, 2L);
        assertThat(response.getPosts()).allSatisfy(post -> assertThat(post.getAuthor()).isSameAs(author));
        assertThat(response.getNextCursor()).isEqualTo(20L);
        assertThat(response.isHasNext()).isTrue();
        var calls = inOrder(users, posts);
        calls.verify(users).getAuthor("owner");
        calls.verify(posts).getPostsByUser("owner", 10L);
        verifyNoMoreInteractions(users, posts);
    }

    @Test
    @DisplayName("게시물이 없으면 빈 목록과 마지막 페이지 정보를 반환한다")
    void emptyPostListIsPreserved() {
        when(users.getAuthor("owner")).thenReturn(AuthorResponse.of(MemberStatus.ACTIVE, "owner"));
        when(posts.getPostsByUser("owner", 0L)).thenReturn(PostListResponse.of(List.of(), null, false));

        var response = facade.getPostsByUser("owner", 0L);

        assertThat(response.getPosts()).isEmpty();
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("작성자 조회 성공 후 게시물 조회가 실패해도 실패를 전파한다")
    void postLookupFailureIsPropagated() {
        when(users.getAuthor("owner")).thenReturn(AuthorResponse.of(MemberStatus.ACTIVE, "owner"));
        var failure = mock(FeignException.ServiceUnavailable.class);
        when(posts.getPostsByUser("owner", 0L)).thenThrow(failure);

        assertThatThrownBy(() -> facade.getPostsByUser("owner", 0L)).isSameAs(failure);
    }

    @Test
    @DisplayName("수정 요청의 대상과 요청자 및 본문을 회원 서비스에 전달한다")
    void updateForwardsRequesterAndRequestWithoutCallingPosts() {
        var request = UpdateProfileRequest.of("new bio", "new@example.com");

        facade.updateProfile("owner", "requester", request);

        verify(users).updateProfile("owner", "requester", request);
        verifyNoMoreInteractions(users);
        verifyNoInteractions(posts);
    }

    @Test
    @DisplayName("회원 서비스의 탈퇴 거부를 성공으로 처리하지 않는다")
    void withdrawalFailureIsPropagatedWithoutCallingPosts() {
        var failure = mock(FeignException.Forbidden.class);
        doThrow(failure).when(users).delete("owner", "other");

        assertThatThrownBy(() -> facade.delete("owner", "other")).isSameAs(failure);
        verifyNoInteractions(posts);
    }

    @Test
    void withdrawnAuthorIsAttachedWithoutChangingPostsOrPagination() {
        when(users.getAuthor("owner")).thenReturn(AuthorResponse.of(MemberStatus.WITHDRAWN, "(탈퇴한 회원)"));
        PostSummary post = PostSummary.of(1L, "thumbnail", 2, 3, null, null, null);
        when(posts.getPostsByUser("owner", 10L)).thenReturn(PostListResponse.of(List.of(post), 20L, true));

        var response = facade.getPostsByUser("owner", 10L);

        assertThat(response.getNextCursor()).isEqualTo(20L);
        assertThat(response.isHasNext()).isTrue();
        var result = response.getPosts().get(0);
        assertThat(result.getPostId()).isEqualTo(1L);
        assertThat(result.getAuthor().getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(result.getAuthor().getDisplayName()).isEqualTo("(탈퇴한 회원)");
        assertThat(post.getAuthor()).isNull();
    }

    @Test
    void authorLookupFailureIsPropagatedInsteadOfInventingWithdrawnStatus() {
        var failure = new IllegalStateException("user-service unavailable");
        when(users.getAuthor("owner")).thenThrow(failure);

        assertThatThrownBy(() -> facade.getPostsByUser("owner", 0L)).isSameAs(failure);
        verifyNoInteractions(posts);
    }

    @Test
    void withdrawalOnlyCallsUserService() {
        facade.delete("owner", "owner");
        verify(users).delete("owner", "owner");
        verifyNoInteractions(posts);
    }
}
