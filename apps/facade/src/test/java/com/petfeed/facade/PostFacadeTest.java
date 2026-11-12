package com.petfeed.facade;

import com.petfeed.contracts.dto.request.SavePostRequest;
import com.petfeed.contracts.dto.response.*;
import com.petfeed.facade.client.*;
import com.petfeed.facade.controller.PostFacade;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostFacadeTest {
    private final PostFeignClient posts = mock(PostFeignClient.class);
    private final MemberFeignClient members = mock(MemberFeignClient.class);
    private final CommentFeignClient comments = mock(CommentFeignClient.class);
    private final PostFacade facade = new PostFacade(posts, members, mock(MediaFeignClient.class));

    @Test void withdrawnAuthorIsDisplayedAndOriginalPostIsPreserved() {
        var original = PostDetailResponse.of(1L, "owner", "hello", null, null, null, null);
        var author = AuthorResponse.of(MemberStatus.WITHDRAWN, "(탈퇴한 회원)");
        when(posts.get(1L)).thenReturn(original);
        when(members.getAuthor("owner")).thenReturn(author);
        assertThat(facade.get(1L).getAuthor()).isSameAs(author);
        assertThat(original.getAuthor()).isNull();
    }

    @Test void withdrawnMemberCannotWrite() {
        when(members.getAuthor("owner")).thenReturn(AuthorResponse.of(MemberStatus.WITHDRAWN, "(탈퇴한 회원)"));
        var request = SavePostRequest.of("hello", null);
        assertThatThrownBy(() -> facade.create("owner", request)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> facade.update(1L, "owner", request)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> facade.delete(1L, "owner")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(posts);
    }

    @Test void memberLookupFailurePreventsCreation() {
        when(members.getAuthor("owner")).thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(() -> facade.create("owner", SavePostRequest.of("hello", null)))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(posts);
    }

    @Test void activeMemberIsCheckedBeforeCreation() {
        var author = AuthorResponse.of(MemberStatus.ACTIVE, "owner");
        var request = SavePostRequest.of("hello", null);
        when(members.getAuthor("owner")).thenReturn(author);
        when(posts.create("owner", request)).thenReturn(PostDetailResponse.of(1L, "owner", "hello", null, null, null, null));
        assertThat(facade.create("owner", request).getAuthor()).isSameAs(author);
        var order = inOrder(members, posts);
        order.verify(members).getAuthor("owner");
        order.verify(posts).create("owner", request);
    }
}
