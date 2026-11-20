package com.petfeed.facade.feed;

import com.petfeed.contracts.dto.response.FeedCandidate;
import java.time.Duration;
import java.util.*;

/** A stable ordering for one browsing session. No database random sort. */
public final class FeedRanker {
    private FeedRanker() {}
    private static final Set<Integer> DISCOVERY_SLOTS = Set.of(2, 5, 8);

    public static List<FeedCandidate> rank(List<FeedCandidate> candidates, Set<String> following, long seed) {
        if (candidates.isEmpty()) return List.of();
        var newest = candidates.stream().map(FeedCandidate::createdAt).max(Comparator.naturalOrder()).orElseThrow();
        var followed = new ArrayList<FeedCandidate>();
        var discovery = new ArrayList<FeedCandidate>();
        var seen = new HashSet<Long>();
        for (var candidate : candidates) {
            if (seen.add(candidate.postId())) (following.contains(candidate.memberKey()) ? followed : discovery).add(candidate);
        }
        followed.sort(Comparator.comparing(FeedCandidate::createdAt).reversed()
                .thenComparing(FeedCandidate::postId, Comparator.reverseOrder()));
        discovery.sort(Comparator.<FeedCandidate>comparingDouble(p -> {
            double ageDays = Math.max(0, Duration.between(p.createdAt(), newest).toSeconds()) / 86400.0;
            return 0.6 * Math.exp(-ageDays / 7.0) + 0.4 * new SplittableRandom(seed ^ p.postId()).nextDouble();
        }).reversed().thenComparing(FeedCandidate::postId));
        var result = new ArrayList<FeedCandidate>();
        String previous = null;
        while (!followed.isEmpty() || !discovery.isEmpty()) {
            var preferred = DISCOVERY_SLOTS.contains(result.size() % 10) ? discovery : followed;
            var fallback = preferred == discovery ? followed : discovery;
            var selected = takeDifferent(preferred, previous);
            if (selected == null) selected = takeDifferent(fallback, previous);
            if (selected == null) selected = (preferred.isEmpty() ? fallback : preferred).removeFirst();
            result.add(selected);
            previous = selected.memberKey();
        }
        return List.copyOf(result);
    }

    private static FeedCandidate takeDifferent(List<FeedCandidate> pool, String previous) {
        for (int i = 0; i < pool.size(); i++) {
            if (!pool.get(i).memberKey().equals(previous)) return pool.remove(i);
        }
        return null;
    }
}
