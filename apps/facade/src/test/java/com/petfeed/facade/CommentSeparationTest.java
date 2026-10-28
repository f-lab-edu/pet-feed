package com.petfeed.facade;

import com.petfeed.domain.dto.request.SaveCommentRequest;
import com.petfeed.domain.dto.response.*;
import com.petfeed.facade.client.*;
import com.petfeed.facade.controller.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommentSeparationTest {
    private final PostFeignClient posts = mock(PostFeignClient.class);
    private final CommentFeignClient comments = mock(CommentFeignClient.class);
    private final UserFeignClient users = mock(UserFeignClient.class);
    private final PostFacade postFacade = new PostFacade(posts, users, comments);
    private final CommentFacade commentFacade = new CommentFacade(comments, users, posts);

    private void activeOwner() {
        when(users.getAuthor("owner")).thenReturn(AuthorResponse.of(MemberStatus.ACTIVE, "owner"));
        when(posts.get(1L)).thenReturn(PostDetailResponse.of(1L, "owner", "post", null, null, null, null));
    }

    @Test void missingPostPreventsCommentCreation() {
        when(users.getAuthor("owner")).thenReturn(AuthorResponse.of(MemberStatus.ACTIVE, "owner"));
        when(posts.get(1L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> commentFacade.create(1L, "owner", SaveCommentRequest.of("hello")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(comments);
    }

    @Test void creationChecksMemberAndPostBeforeWritingComment() {
        activeOwner();
        var request = SaveCommentRequest.of("hello");
        when(comments.create(1L, "owner", request)).thenReturn(
                CommentResponse.of(2L, 1L, "owner", "hello", null, null, null));
        assertThat(commentFacade.create(1L, "owner", request).getAuthor().getStatus()).isEqualTo(MemberStatus.ACTIVE);
        var order = inOrder(users, posts, comments);
        order.verify(users).getAuthor("owner");
        order.verify(posts).get(1L);
        order.verify(comments).create(1L, "owner", request);
    }

    @Test void missingPostPreventsCommentListing() {
        when(posts.get(1L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> commentFacade.list(1L, 0L)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(comments, users);
    }

    @Test void unauthorizedPostDeletionDoesNotTouchComments() {
        when(users.getAuthor("other")).thenReturn(AuthorResponse.of(MemberStatus.ACTIVE, "other"));
        when(posts.get(1L)).thenReturn(PostDetailResponse.of(1L, "owner", "post", null, null, null, null));
        assertThatThrownBy(() -> postFacade.delete(1L, "other")).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(comments);
        verify(posts, never()).delete(anyLong(), anyString());
    }

    @Test void commentCleanupFailureKeepsPostAndRetryCompletesDeletion() {
        activeOwner();
        var failure = new IllegalStateException("comment service unavailable");
        doThrow(failure).doNothing().when(comments).close(1L);
        assertThatThrownBy(() -> postFacade.delete(1L, "owner")).isSameAs(failure);
        verify(posts, never()).delete(anyLong(), anyString());
        postFacade.delete(1L, "owner");
        var order = inOrder(comments, posts);
        order.verify(posts).get(1L);
        order.verify(comments).close(1L);
        order.verify(posts).get(1L);
        order.verify(comments).close(1L);
        order.verify(posts).delete(1L, "owner");
    }

    @Test void failedPostDeletionLeavesCommentsClosedAndRetryFinishes() {
        activeOwner();
        var closed = new AtomicBoolean();
        var exists = new AtomicBoolean(true);
        doAnswer(call -> { closed.set(true); return null; }).when(comments).close(1L);
        doThrow(new IllegalStateException("post service unavailable"))
                .doAnswer(call -> { exists.set(false); return null; }).when(posts).delete(1L, "owner");
        assertThatThrownBy(() -> postFacade.delete(1L, "owner")).isInstanceOf(IllegalStateException.class);
        assertThat(closed).isTrue();
        assertThat(exists).isTrue();
        postFacade.delete(1L, "owner");
        assertThat(exists).isFalse();
        verify(comments, times(2)).close(1L);
    }

    @Test void countsAreComposedInOneCallAndOriginalSummaryIsPreserved() {
        var facade = new UserFacade(users, posts, comments);
        when(users.getAuthor("owner")).thenReturn(AuthorResponse.of(MemberStatus.ACTIVE, "owner"));
        var original = PostSummary.of(1L, null, 0, 0, null, null, null);
        when(posts.getPostsByUser("owner", 0L)).thenReturn(PostListResponse.of(List.of(original,
                PostSummary.of(2L, null, 0, 0, null, null, null)), null, false));
        when(comments.counts(List.of(1L, 2L))).thenReturn(Map.of(1L, 3L));
        assertThat(facade.getPostsByUser("owner", 0L).getPosts()).extracting(PostSummary::getCommentCount)
                .containsExactly(3, 0);
        assertThat(original.getCommentCount()).isZero();
        verify(comments).counts(List.of(1L, 2L));
    }

    @Test void countServiceFailureIsNotReportedAsZeroComments() {
        var facade = new UserFacade(users, posts, comments);
        when(users.getAuthor("owner")).thenReturn(AuthorResponse.of(MemberStatus.ACTIVE, "owner"));
        when(posts.getPostsByUser("owner", 0L)).thenReturn(PostListResponse.of(List.of(
                PostSummary.of(1L, null, 0, 0, null, null, null)), null, false));
        var failure = new IllegalStateException("counts unavailable");
        when(comments.counts(List.of(1L))).thenThrow(failure);
        assertThatThrownBy(() -> facade.getPostsByUser("owner", 0L)).isSameAs(failure);
    }
}
