package com.petfeed.facade;

import com.petfeed.domain.dto.request.SaveCommentRequest;
import com.petfeed.domain.dto.response.*;
import com.petfeed.facade.client.*;
import com.petfeed.facade.controller.CommentFacade;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;

class CommentFacadeTest {
    private final CommentFeignClient comments = mock(CommentFeignClient.class);
    private final UserFeignClient users = mock(UserFeignClient.class);
    private final CommentFacade facade = new CommentFacade(comments, users);

    @Test void withdrawnMemberCannotWrite() {
        when(users.getAuthor("reader")).thenReturn(AuthorResponse.of(MemberStatus.WITHDRAWN, "withdrawn"));
        var request = SaveCommentRequest.of("hello");
        assertThatThrownBy(() -> facade.create(1L, "reader", request)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> facade.update(1L, 2L, "reader", request)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> facade.delete(1L, 2L, "reader")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(comments);
    }

    @Test void listEnrichesWithdrawnAuthorAndCachesRepeatedLookup() {
        var author = AuthorResponse.of(MemberStatus.WITHDRAWN, "withdrawn");
        when(users.getAuthor("reader")).thenReturn(author);
        var first = CommentResponse.of(2L, 1L, "reader", "hello", null, null, null);
        var second = CommentResponse.of(1L, 1L, "reader", "earlier", null, null, null);
        when(comments.list(1L, 0L)).thenReturn(CommentListResponse.of(List.of(first, second), null, false));
        assertThat(facade.list(1L, 0L).getComments()).allSatisfy(comment -> assertThat(comment.getAuthor()).isSameAs(author));
        verify(users, times(1)).getAuthor("reader");
        assertThat(first.getAuthor()).isNull();
    }
}
