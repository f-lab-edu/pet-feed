package com.petfeed.facade.controller;

import com.petfeed.contracts.dto.response.*;
import com.petfeed.facade.client.*;
import com.petfeed.facade.feed.FeedRanker;
import com.petfeed.facade.feed.FeedSessions;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FeedFacade {

    private final PostFeignClient posts;
    private final FollowFeignClient follows;
    private final MemberFeignClient members;
    private final CommentFeignClient comments;
    private final FeedSessions sessions;

    public FeedResponse get(String requester, String cursor) {

        if (members.getAuthor(requester).getStatus() != AuthorStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active membership required");

        var page = cursor == null ? start(requester) : sessions.resume(requester, cursor);
        var snapshot = page.snapshot();
        int end = Math.min(page.offset() + 10, snapshot.ordered().size());
        var feedIds = getFeedList(snapshot, page, end);
        var details = getDetails(feedIds);
        var counts = details.isEmpty() ? Map.<Long, Long>of() : comments.counts(new ArrayList<>(details.keySet()));
        var authors = new HashMap<String, AuthorResponse>();
        var items = new ArrayList<FeedItem>();

        for (var id : feedIds) {
            var post = details.get(id);
            if (post == null)
                continue;
            var author = authors.computeIfAbsent(post.getMemberKey(), members::getAuthor);
            items.add(new FeedItem(post.withAuthor(author), counts.getOrDefault(id, 0L),
                    snapshot.following().contains(post.getMemberKey()) ? "FOLLOWING" : "DISCOVERY"));
        }

        boolean more = end < snapshot.ordered().size();
        return new FeedResponse(List.copyOf(items), more ? page.id() + ":" + end : null, more);
    }

    private @NonNull List<Long> getFeedList(FeedSessions.Snapshot snapshot, FeedSessions.Page page, int end) {
        return snapshot.ordered()
                .subList(page.offset(), end).stream()
                .map(FeedCandidate::postId)
                .toList();
    }

    private @NonNull Map<Long, PostDetailResponse> getDetails(List<Long> ids) {
        return ids.isEmpty() ? Map.<Long, PostDetailResponse>of() : posts.feedDetails(ids).stream()
                .collect(Collectors.toMap(PostDetailResponse::getPostId, p -> p));
    }

    private FeedSessions.Page start(String requester) {

        var candidates = posts.feedCandidates(requester);
        var keys = candidates.stream()
                .map(FeedCandidate::memberKey)
                .distinct()
                .toList();

        var following = new HashSet<String>();
        for (int i = 0; i < keys.size(); i += 20)
            following.addAll(follows.targets(requester, keys.subList(i, Math.min(i + 20, keys.size()))));

        return sessions.create(requester, FeedRanker.rank(candidates, following, UUID.randomUUID().getMostSignificantBits()), following);
    }
}
