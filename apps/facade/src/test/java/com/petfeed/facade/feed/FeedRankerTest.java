package com.petfeed.facade.feed;

import com.petfeed.contracts.dto.response.FeedCandidate;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;

class FeedRankerTest {
    private List<FeedCandidate> candidates() {
        return IntStream.rangeClosed(1, 100).mapToObj(i -> new FeedCandidate((long) i,
                "author" + i, Instant.parse("2026-11-20T12:00:00Z").minusSeconds(i * 60L))).toList();
    }

    @Test void mixesSevenFollowedAndThreeDiscoveriesWhenBothPoolsHaveCapacity() {
        var follows = new HashSet<String>();
        for (int i = 1; i <= 70; i++) follows.add("author" + i);
        var ranked = FeedRanker.rank(candidates(), follows, 42);
        for (int i = 0; i < 100; i += 10) {
            assertThat(ranked.subList(i, i + 10).stream().filter(p -> follows.contains(p.memberKey())).count()).isEqualTo(7);
        }
        assertThat(ranked).hasSize(100).doesNotHaveDuplicates();
        assertThat(ranked.stream().filter(p -> follows.contains(p.memberKey())).map(FeedCandidate::postId).toList())
                .isSorted();
    }

    @Test void seedIsRepeatableAndNewSeedChangesDiscoveryOrder() {
        assertThat(FeedRanker.rank(candidates(), Set.of(), 42)).isEqualTo(FeedRanker.rank(candidates(), Set.of(), 42));
        assertThat(FeedRanker.rank(candidates(), Set.of(), 42)).isNotEqualTo(FeedRanker.rank(candidates(), Set.of(), 43));
    }

    @Test void preventsAdjacentAuthorsWhenAlternativesExistAndFallsBackWithoutLosingPosts() {
        var now = Instant.parse("2026-11-20T12:00:00Z");
        var pool = List.of(new FeedCandidate(1L, "a", now), new FeedCandidate(2L, "a", now),
                new FeedCandidate(3L, "b", now), new FeedCandidate(4L, "b", now));
        var ranked = FeedRanker.rank(pool, Set.of("a", "b"), 1);
        for (int i = 1; i < ranked.size(); i++) assertThat(ranked.get(i).memberKey()).isNotEqualTo(ranked.get(i - 1).memberKey());
        assertThat(ranked).containsExactlyInAnyOrderElementsOf(pool);
        assertThat(FeedRanker.rank(List.of(pool.getFirst(), pool.getFirst()), Set.of(), 1)).hasSize(1);
        assertThat(FeedRanker.rank(List.of(), Set.of(), 1)).isEmpty();
    }

    @Test void recencyUsesElapsedTimeAcrossOffsetsAndDaylightSavingTransition() {
        var before = OffsetDateTime.parse("2026-03-08T01:30:00-05:00").toInstant();
        var after = OffsetDateTime.parse("2026-03-08T03:30:00-04:00").toInstant();
        var post = new FeedCandidate(1L, "a", before);
        double random = 0.4 * new SplittableRandom(42L ^ 1L).nextDouble();
        assertThat(FeedRanker.discoveryScore(post, after, 42))
                .isCloseTo(0.6 * Math.exp(-(1.0 / 24) / 7) + random, within(1e-12));
        var sameInstant = OffsetDateTime.parse("2026-03-08T15:30:00+09:00").toInstant();
        assertThat(FeedRanker.discoveryScore(new FeedCandidate(1L, "a", sameInstant), after, 42))
                .isEqualTo(FeedRanker.discoveryScore(post, after, 42));
    }
}
