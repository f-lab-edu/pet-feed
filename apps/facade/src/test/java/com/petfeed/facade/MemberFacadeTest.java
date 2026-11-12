package com.petfeed.facade;

import com.petfeed.contracts.dto.response.*;
import com.petfeed.facade.client.PostFeignClient;
import com.petfeed.facade.client.MemberFeignClient;
import com.petfeed.facade.controller.MemberFacade;
import com.petfeed.contracts.dto.request.UpdateProfileRequest;
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
class MemberFacadeTest {
    @Mock MemberFeignClient members;
    @Mock PostFeignClient posts;
    @Mock com.petfeed.facade.client.CommentFeignClient comments;
    @Mock com.petfeed.facade.controller.FollowFacade follows;
    @InjectMocks MemberFacade facade;

    @Test
    @DisplayName("프로필과 게시물 수를 조합하고 원본 프로필은 변경하지 않는다")
    void profileCombinesMemberInformationAndPostCount() {
        var profile = MemberInfoResponse.of("owner", "image", "bio");
        when(members.getProfile("owner")).thenReturn(profile);
        when(posts.getPostCount("owner")).thenReturn(12L);
        when(follows.statistics("owner", "viewer")).thenReturn(new com.petfeed.facade.model.FollowStatistics(5, 7, true));

        var response = facade.getProfile("owner", "viewer");

        assertThat(response).usingRecursiveComparison().isEqualTo(
                MemberProfileResponse.of("owner", "image", "bio", 5, 7, true, 12));
        assertThat(profile.getNickname()).isEqualTo("owner");
        verify(follows).statistics("owner", "viewer");
        verify(members).getProfile("owner");
        verify(posts).getPostCount("owner");
        verifyNoMoreInteractions(members, posts);
    }

    @Test
    @DisplayName("프로필 조회 실패 시 게시물 수 조회 없이 실패를 전파한다")
    void profileLookupFailureStopsOrchestration() {
        var failure = mock(FeignException.NotFound.class);
        when(members.getProfile("missing")).thenThrow(failure);

        assertThatThrownBy(() -> facade.getProfile("missing", "viewer")).isSameAs(failure);
        verifyNoInteractions(posts);
    }

    @Test
    @DisplayName("필수 게시물 수 조회 실패 시 불완전한 프로필을 반환하지 않는다")
    void postCountFailureIsPropagated() {
        when(members.getProfile("owner")).thenReturn(
                MemberInfoResponse.of("owner", null, null));
        var failure = mock(FeignException.ServiceUnavailable.class);
        when(posts.getPostCount("owner")).thenThrow(failure);

        assertThatThrownBy(() -> facade.getProfile("owner", "viewer")).isSameAs(failure);
    }

    @Test
    @DisplayName("정상 작성자 정보를 목록 전체에 조합하고 커서를 전달한다")
    void activeAuthorIsAttachedToEveryPost() {
        var author = AuthorResponse.of(AuthorStatus.ACTIVE, "owner");
        when(members.getAuthor("owner")).thenReturn(author);
        when(posts.getPostsByMember("owner", 10L)).thenReturn(PostListResponse.of(List.of(
                PostSummary.of(1L, "first", 2, 3, null, null, null),
                PostSummary.of(2L, "second", 4, 5, null, null, null)), 20L, true));

        var response = facade.getPostsByMember("owner", 10L);

        assertThat(response.getPosts()).extracting(PostSummary::getPostId).containsExactly(1L, 2L);
        assertThat(response.getPosts()).allSatisfy(post -> assertThat(post.getAuthor()).isSameAs(author));
        assertThat(response.getNextCursor()).isEqualTo(20L);
        assertThat(response.isHasNext()).isTrue();
        var calls = inOrder(members, posts);
        calls.verify(members).getAuthor("owner");
        calls.verify(posts).getPostsByMember("owner", 10L);
        verifyNoMoreInteractions(members, posts);
    }

    @Test
    @DisplayName("게시물이 없으면 빈 목록과 마지막 페이지 정보를 반환한다")
    void emptyPostListIsPreserved() {
        when(members.getAuthor("owner")).thenReturn(AuthorResponse.of(AuthorStatus.ACTIVE, "owner"));
        when(posts.getPostsByMember("owner", 0L)).thenReturn(PostListResponse.of(List.of(), null, false));

        var response = facade.getPostsByMember("owner", 0L);

        assertThat(response.getPosts()).isEmpty();
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("작성자 조회 성공 후 게시물 조회가 실패해도 실패를 전파한다")
    void postLookupFailureIsPropagated() {
        when(members.getAuthor("owner")).thenReturn(AuthorResponse.of(AuthorStatus.ACTIVE, "owner"));
        var failure = mock(FeignException.ServiceUnavailable.class);
        when(posts.getPostsByMember("owner", 0L)).thenThrow(failure);

        assertThatThrownBy(() -> facade.getPostsByMember("owner", 0L)).isSameAs(failure);
    }

    @Test
    @DisplayName("수정 요청의 대상과 요청자 및 본문을 회원 서비스에 전달한다")
    void updateForwardsRequesterAndRequestWithoutCallingPosts() {
        var request = UpdateProfileRequest.of("new bio", "new@example.com");

        facade.updateProfile("owner", "requester", request);

        verify(members).updateProfile("owner", "requester", request);
        verifyNoMoreInteractions(members);
        verifyNoInteractions(posts);
    }

    @Test
    @DisplayName("회원 서비스의 탈퇴 거부를 성공으로 처리하지 않는다")
    void withdrawalFailureIsPropagatedWithoutCallingPosts() {
        var failure = mock(FeignException.Forbidden.class);
        doThrow(failure).when(members).delete("owner", "other");

        assertThatThrownBy(() -> facade.delete("owner", "other")).isSameAs(failure);
        verifyNoInteractions(posts);
    }

    @Test
    void withdrawnAuthorIsAttachedWithoutChangingPostsOrPagination() {
        when(members.getAuthor("owner")).thenReturn(AuthorResponse.of(AuthorStatus.WITHDRAWN, "(탈퇴한 회원)"));
        PostSummary post = PostSummary.of(1L, "thumbnail", 2, 3, null, null, null);
        when(posts.getPostsByMember("owner", 10L)).thenReturn(PostListResponse.of(List.of(post), 20L, true));

        var response = facade.getPostsByMember("owner", 10L);

        assertThat(response.getNextCursor()).isEqualTo(20L);
        assertThat(response.isHasNext()).isTrue();
        var result = response.getPosts().get(0);
        assertThat(result.getPostId()).isEqualTo(1L);
        assertThat(result.getAuthor().getStatus()).isEqualTo(AuthorStatus.WITHDRAWN);
        assertThat(result.getAuthor().getDisplayName()).isEqualTo("(탈퇴한 회원)");
        assertThat(post.getAuthor()).isNull();
    }

    @Test
    void authorLookupFailureIsPropagatedInsteadOfInventingWithdrawnStatus() {
        var failure = new IllegalStateException("member-service unavailable");
        when(members.getAuthor("owner")).thenThrow(failure);

        assertThatThrownBy(() -> facade.getPostsByMember("owner", 0L)).isSameAs(failure);
        verifyNoInteractions(posts);
    }

    @Test
    void withdrawalOnlyCallsMemberService() {
        facade.delete("owner", "owner");
        verify(members).delete("owner", "owner");
        verifyNoInteractions(posts);
    }
}
