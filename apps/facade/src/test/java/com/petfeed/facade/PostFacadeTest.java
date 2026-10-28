package com.petfeed.facade;

import com.petfeed.domain.dto.request.SavePostRequest;
import com.petfeed.domain.dto.response.*;
import com.petfeed.facade.client.*;
import com.petfeed.facade.controller.PostFacade;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostFacadeTest {
    private final PostFeignClient posts = mock(PostFeignClient.class);
    private final UserFeignClient users = mock(UserFeignClient.class);
    private final PostFacade facade = new PostFacade(posts, users);

    @Test void withdrawnAuthorIsDisplayedAndOriginalPostIsPreserved() {
        var original = PostDetailResponse.of(1L, "owner", "hello", null, null, null, null);
        var author = AuthorResponse.of(MemberStatus.WITHDRAWN, "(탈퇴한 회원)");
        when(posts.get(1L)).thenReturn(original);
        when(users.getAuthor("owner")).thenReturn(author);
        assertThat(facade.get(1L).getAuthor()).isSameAs(author);
        assertThat(original.getAuthor()).isNull();
    }

    @Test void withdrawnMemberCannotWrite() {
        when(users.getAuthor("owner")).thenReturn(AuthorResponse.of(MemberStatus.WITHDRAWN, "(탈퇴한 회원)"));
        var request = SavePostRequest.of("hello", null);
        assertThatThrownBy(() -> facade.create("owner", request)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> facade.update(1L, "owner", request)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> facade.delete(1L, "owner")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(posts);
    }

    @Test void memberLookupFailurePreventsCreation() {
        when(users.getAuthor("owner")).thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(() -> facade.create("owner", SavePostRequest.of("hello", null)))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(posts);
    }

    @Test void activeMemberIsCheckedBeforeCreation() {
        var author = AuthorResponse.of(MemberStatus.ACTIVE, "owner");
        var request = SavePostRequest.of("hello", null);
        when(users.getAuthor("owner")).thenReturn(author);
        when(posts.create("owner", request)).thenReturn(PostDetailResponse.of(1L, "owner", "hello", null, null, null, null));
        assertThat(facade.create("owner", request).getAuthor()).isSameAs(author);
        var order = inOrder(users, posts);
        order.verify(users).getAuthor("owner");
        order.verify(posts).create("owner", request);
    }
}
