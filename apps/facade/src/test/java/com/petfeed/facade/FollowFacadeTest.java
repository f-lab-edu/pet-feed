package com.petfeed.facade;

import com.petfeed.facade.client.*;
import com.petfeed.facade.controller.FollowFacade;
import com.petfeed.contracts.dto.response.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FollowFacadeTest {
    private final FollowFeignClient follows = mock(FollowFeignClient.class);
    private final MemberFeignClient members = mock(MemberFeignClient.class);
    private final FollowFacade facade = new FollowFacade(follows, members);

    private void active(String key) { when(members.getAuthor(key)).thenReturn(AuthorResponse.of(AuthorStatus.ACTIVE, key)); }

    @Test void createRequiresBothActiveMembersAndForwardsRequester() {
        active("owner"); active("target");
        facade.follow("owner", "target");
        var order = inOrder(members, follows);
        order.verify(members).getAuthor("owner");
        order.verify(members).getAuthor("target");
        order.verify(follows).follow("target", "owner");
    }

    @Test void selfFollowAndWithdrawnTargetNeverWrite() {
        assertThatThrownBy(() -> facade.follow("owner", "owner")).isInstanceOf(ResponseStatusException.class);
        active("owner");
        when(members.getAuthor("target")).thenReturn(AuthorResponse.of(AuthorStatus.WITHDRAWN, "withdrawn"));
        assertThatThrownBy(() -> facade.follow("owner", "target")).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(follows);
    }

    @Test void membershipLookupFailureIsNotMisreadAsWithdrawal() {
        active("owner");
        var failure = new IllegalStateException("member service unavailable");
        when(members.getAuthor("target")).thenThrow(failure);
        assertThatThrownBy(() -> facade.follow("owner", "target")).isSameAs(failure);
        verifyNoInteractions(follows);
    }

    @Test void unfollowCanRemoveRelationshipToWithdrawnTarget() {
        active("owner");
        facade.unfollow("owner", "withdrawn");
        verify(follows).unfollow("withdrawn", "owner");
        verify(members, never()).getAuthor("withdrawn");
    }

    @Test void filteringAcrossRawPagesPreservesNextCursorAndFollowingFlags() {
        active("owner"); active("viewer");
        var inactive = new ArrayList<FollowEntry>();
        for (int i = 0; i < 20; i++) {
            String key = "withdrawn" + i;
            when(members.getAuthor(key)).thenReturn(AuthorResponse.of(AuthorStatus.WITHDRAWN, "withdrawn"));
            inactive.add(FollowEntry.of(100L - i, key));
        }
        var activeEntries = new ArrayList<FollowEntry>();
        for (int i = 0; i < 21; i++) { active("friend" + i); activeEntries.add(FollowEntry.of(80L - i, "friend" + i)); }
        when(follows.followers("owner", 0L)).thenReturn(FollowPage.of(inactive, 81L, true));
        when(follows.followers("owner", 81L)).thenReturn(FollowPage.of(activeEntries.subList(0, 20), 61L, true));
        when(follows.followers("owner", 61L)).thenReturn(FollowPage.of(List.of(activeEntries.getLast()), null, false));
        when(follows.targets(eq("viewer"), anyList())).thenReturn(Set.of("friend0"));
        var first = facade.list("owner", "viewer", 0L, true);
        assertThat(first.getMembers()).hasSize(20);
        assertThat(first.getMembers().getFirst().isFollowing()).isTrue();
        assertThat(first.isHasNext()).isTrue();
        assertThat(first.getNextCursor()).isEqualTo(61L);
        var last = facade.list("owner", "viewer", first.getNextCursor(), true);
        assertThat(last.getMembers()).extracting(MemberSummary::getMemberKey).containsExactly("friend20");
        assertThat(last.isHasNext()).isFalse();
    }

    @Test void profileCountsOnlyActiveRelationshipsAndUsesRequesterForFollowingFlag() {
        active("owner"); active("viewer"); active("friend");
        when(members.getAuthor("withdrawn")).thenReturn(AuthorResponse.of(AuthorStatus.WITHDRAWN, "withdrawn"));
        when(follows.followers("owner", 0L)).thenReturn(FollowPage.of(List.of(
                FollowEntry.of(2L, "friend"), FollowEntry.of(1L, "withdrawn")), null, false));
        when(follows.followings("owner", 0L)).thenReturn(FollowPage.of(List.of(FollowEntry.of(3L, "withdrawn")), null, false));
        when(follows.targets("viewer", List.of("owner"))).thenReturn(Set.of("owner"));
        var profile = facade.statistics("owner", "viewer");
        assertThat(profile.followerCount()).isEqualTo(1);
        assertThat(profile.followingCount()).isZero();
        assertThat(profile.following()).isTrue();

        verify(members, times(1)).getAuthor("withdrawn");
    }
}
