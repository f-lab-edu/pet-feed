package com.petfeed.facade.feed;

import com.petfeed.contracts.dto.response.*;
import com.petfeed.facade.client.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeedFacadeTest {
    private final PostFeignClient posts = mock(PostFeignClient.class);
    private final FollowFeignClient follows = mock(FollowFeignClient.class);
    private final MemberFeignClient members = mock(MemberFeignClient.class);
    private final CommentFeignClient comments = mock(CommentFeignClient.class);
    private final FeedFacade feed = new FeedFacade(posts, follows, members, comments, new FeedSessions());

    @Test void pagesDoNotReshuffleAndDeletedPostsAreNotServedFromSnapshot() {
        var now = LocalDateTime.of(2026, 11, 20, 0, 0);
        var candidates = IntStream.rangeClosed(1, 25).mapToObj(i -> new FeedCandidate((long) i, "a" + i, now)).toList();
        when(members.getAuthor(anyString())).thenAnswer(call -> AuthorResponse.of(AuthorStatus.ACTIVE, call.getArgument(0)));
        when(posts.feedCandidates("viewer")).thenReturn(candidates);
        when(follows.targets(eq("viewer"), anyList())).thenReturn(Set.of("a1", "a2"));
        var removed = new HashSet<Long>();
        when(posts.feedDetails(anyList())).thenAnswer(call -> ((List<Long>) call.getArgument(0)).stream()
                .filter(id -> !removed.contains(id)).map(id -> PostDetailResponse.of(id, "a" + id, "content", null, now, now, null)).toList());
        when(comments.counts(anyList())).thenReturn(Map.of(1L, 3L));
        var first = feed.get("viewer", null);
        var second = feed.get("viewer", first.nextCursor());
        assertThat(second.items().stream().map(item -> item.post().getPostId()).toList())
                .isEqualTo(feed.get("viewer", first.nextCursor()).items().stream().map(item -> item.post().getPostId()).toList());
        var ids = new HashSet<Long>();
        first.items().forEach(item -> assertThat(ids.add(item.post().getPostId())).isTrue());
        second.items().forEach(item -> assertThat(ids.add(item.post().getPostId())).isTrue());
        removed.add(second.items().getFirst().post().getPostId());
        assertThat(feed.get("viewer", first.nextCursor()).items()).hasSize(9);
        var last = feed.get("viewer", second.nextCursor());
        assertThat(last.items()).hasSize(5);
        assertThat(last.hasNext()).isFalse();
        verify(posts, times(1)).feedCandidates("viewer");
        verify(follows, times(2)).targets(eq("viewer"), argThat(keys -> keys.size() <= 20));
    }

    @Test void downstreamFailureIsNotCachedAsAnEmptyFeed() {
        when(members.getAuthor("viewer")).thenReturn(AuthorResponse.of(AuthorStatus.ACTIVE, "viewer"));
        when(posts.feedCandidates("viewer")).thenThrow(new IllegalStateException("Unavailable"));
        assertThatThrownBy(() -> feed.get("viewer", null)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(follows, comments);
    }
}
