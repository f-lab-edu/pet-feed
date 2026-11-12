package com.petfeed.facade;

import com.petfeed.contracts.dto.request.SaveCommentRequest;
import com.petfeed.contracts.dto.response.*;
import com.petfeed.facade.client.*;
import com.petfeed.facade.controller.CommentFacade;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;

class CommentFacadeTest {
    private final CommentFeignClient comments = mock(CommentFeignClient.class);
    private final MemberFeignClient members = mock(MemberFeignClient.class);
    private final PostFeignClient posts = mock(PostFeignClient.class);
    private final CommentFacade facade = new CommentFacade(comments, members, posts);

    @Test void withdrawnMemberCannotWrite() {
        when(members.getAuthor("reader")).thenReturn(AuthorResponse.of(AuthorStatus.WITHDRAWN, "withdrawn"));
        var request = SaveCommentRequest.of("hello");
        assertThatThrownBy(() -> facade.create(1L, "reader", request)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> facade.update(1L, 2L, "reader", request)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> facade.delete(1L, 2L, "reader")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(comments);
    }

    @Test void listEnrichesWithdrawnAuthorAndCachesRepeatedLookup() {
        var author = AuthorResponse.of(AuthorStatus.WITHDRAWN, "withdrawn");
        when(members.getAuthor("reader")).thenReturn(author);
        var first = CommentResponse.of(2L, 1L, "reader", "hello", null, null, null);
        var second = CommentResponse.of(1L, 1L, "reader", "earlier", null, null, null);
        when(comments.list(1L, 0L)).thenReturn(CommentListResponse.of(List.of(first, second), null, false));
        assertThat(facade.list(1L, 0L).getComments()).allSatisfy(comment -> assertThat(comment.getAuthor()).isSameAs(author));
        verify(members, times(1)).getAuthor("reader");
        assertThat(first.getAuthor()).isNull();
    }
}
