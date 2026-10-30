package com.petfeed.facade;

import com.petfeed.facade.client.*;
import com.petfeed.facade.controller.FollowFacade;
import com.petfeed.domain.dto.response.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FollowFacadeTest {
    private final FollowFeignClient follows = mock(FollowFeignClient.class);
    private final UserFeignClient users = mock(UserFeignClient.class);
    private final FollowFacade facade = new FollowFacade(follows, users);

    private void active(String key) { when(users.getAuthor(key)).thenReturn(AuthorResponse.of(MemberStatus.ACTIVE, key)); }

    @Test void createRequiresBothActiveMembersAndForwardsRequester() {
        active("owner"); active("target");
        facade.follow("owner", "target");
        var order = inOrder(users, follows);
        order.verify(users).getAuthor("owner");
        order.verify(users).getAuthor("target");
        order.verify(follows).follow("target", "owner");
    }

    @Test void selfFollowAndWithdrawnTargetNeverWrite() {
        assertThatThrownBy(() -> facade.follow("owner", "owner")).isInstanceOf(ResponseStatusException.class);
        active("owner");
        when(users.getAuthor("target")).thenReturn(AuthorResponse.of(MemberStatus.WITHDRAWN, "withdrawn"));
        assertThatThrownBy(() -> facade.follow("owner", "target")).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(follows);
    }

    @Test void membershipLookupFailureIsNotMisreadAsWithdrawal() {
        active("owner");
        var failure = new IllegalStateException("user service unavailable");
        when(users.getAuthor("target")).thenThrow(failure);
        assertThatThrownBy(() -> facade.follow("owner", "target")).isSameAs(failure);
        verifyNoInteractions(follows);
    }

    @Test void unfollowCanRemoveRelationshipToWithdrawnTarget() {
        active("owner");
        facade.unfollow("owner", "withdrawn");
        verify(follows).unfollow("withdrawn", "owner");
        verify(users, never()).getAuthor("withdrawn");
    }

    @Test void filteringAcrossRawPagesPreservesNextCursorAndFollowingFlags() {
        active("owner"); active("viewer");
        var inactive = new ArrayList<FollowEntry>();
        for (int i = 0; i < 20; i++) {
            String key = "withdrawn" + i;
            when(users.getAuthor(key)).thenReturn(AuthorResponse.of(MemberStatus.WITHDRAWN, "withdrawn"));
            inactive.add(FollowEntry.of(100L - i, key));
        }
        var activeEntries = new ArrayList<FollowEntry>();
        for (int i = 0; i < 21; i++) { active("friend" + i); activeEntries.add(FollowEntry.of(80L - i, "friend" + i)); }
        when(follows.followers("owner", 0L)).thenReturn(FollowPage.of(inactive, 81L, true));
        when(follows.followers("owner", 81L)).thenReturn(FollowPage.of(activeEntries.subList(0, 20), 61L, true));
        when(follows.followers("owner", 61L)).thenReturn(FollowPage.of(List.of(activeEntries.getLast()), null, false));
        when(follows.targets(eq("viewer"), anyList())).thenReturn(Set.of("friend0"));
        var first = facade.list("owner", "viewer", 0L, true);
        assertThat(first.getUsers()).hasSize(20);
        assertThat(first.getUsers().getFirst().isFollowing()).isTrue();
        assertThat(first.isHasNext()).isTrue();
        assertThat(first.getNextCursor()).isEqualTo(61L);
        var last = facade.list("owner", "viewer", first.getNextCursor(), true);
        assertThat(last.getUsers()).extracting(UserSummary::getUserKey).containsExactly("friend20");
        assertThat(last.isHasNext()).isFalse();
    }

    @Test void profileCountsOnlyActiveRelationshipsAndUsesRequesterForFollowingFlag() {
        active("owner"); active("viewer"); active("friend");
        when(users.getAuthor("withdrawn")).thenReturn(AuthorResponse.of(MemberStatus.WITHDRAWN, "withdrawn"));
        when(follows.followers("owner", 0L)).thenReturn(FollowPage.of(List.of(
                FollowEntry.of(2L, "friend"), FollowEntry.of(1L, "withdrawn")), null, false));
        when(follows.followings("owner", 0L)).thenReturn(FollowPage.of(List.of(FollowEntry.of(3L, "withdrawn")), null, false));
        when(follows.targets("viewer", List.of("owner"))).thenReturn(Set.of("owner"));
        var profile = facade.profile("owner", "viewer", UserProfileResponse.of("owner", null, "bio", 0, 0, false, 5));
        assertThat(profile.getFollowerCount()).isEqualTo(1);
        assertThat(profile.getFollowingCount()).isZero();
        assertThat(profile.isFollowing()).isTrue();
        assertThat(profile.getPostCount()).isEqualTo(5);
        verify(users, times(1)).getAuthor("withdrawn");
    }
}
