package com.petfeed.facade.controller;

import com.petfeed.facade.client.*;
import com.petfeed.domain.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Component
@RequiredArgsConstructor
public class FollowFacade {
    private final FollowFeignClient follows;
    private final UserFeignClient users;

    public void follow(String requester, String target) {
        if (requester.equals(target)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot follow yourself");
        requireActive(requester, new HashMap<>());
        requireActive(target, new HashMap<>());
        follows.follow(target, requester);
    }

    public void unfollow(String requester, String target) {
        requireActive(requester, new HashMap<>());
        // A surviving member may remove a relationship to a withdrawn/deleted target.
        follows.unfollow(target, requester);
    }

    public UserListResponse list(String key, String requester, Long cursor, boolean followers) {
        if (cursor == null || cursor < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor must be nonnegative");
        var authors = new HashMap<String, AuthorResponse>();
        requireActive(requester, authors);
        requireActive(key, authors);
        var visible = new ArrayList<FollowEntry>();
        scan(key, cursor, followers, authors, entry -> { visible.add(entry); return visible.size() < 21; });
        boolean hasNext = visible.size() > 20;
        var page = visible.stream().limit(20).toList();
        var following = page.isEmpty() ? Set.<String>of() : follows.targets(requester, page.stream().map(FollowEntry::getUserKey).toList());
        var result = page.stream().map(entry -> UserSummary.of(entry.getUserKey(), authors.get(entry.getUserKey()).getDisplayName(),
                null, following.contains(entry.getUserKey()))).toList();
        return UserListResponse.of(result, hasNext ? page.getLast().getFollowId() : null, hasNext);
    }

    public UserProfileResponse profile(String key, String requester, UserProfileResponse base) {
        var authors = new HashMap<String, AuthorResponse>();
        requireActive(requester, authors);
        requireActive(key, authors);
        int[] counts = new int[2];
        scan(key, 0L, true, authors, entry -> { counts[0] = Math.incrementExact(counts[0]); return true; });
        scan(key, 0L, false, authors, entry -> { counts[1] = Math.incrementExact(counts[1]); return true; });
        boolean following = !requester.equals(key) && follows.targets(requester, List.of(key)).contains(key);
        return UserProfileResponse.of(base.getNickname(), base.getProfileImageUrl(), base.getBio(), counts[0], counts[1], following, base.getPostCount());
    }

    private void scan(String key, Long cursor, boolean followers, Map<String, AuthorResponse> authors,
                      java.util.function.Predicate<FollowEntry> visitor) {
        while (true) {
            var page = followers ? follows.followers(key, cursor) : follows.followings(key, cursor);
            for (var entry : page.getEntries()) {
                if (author(entry.getUserKey(), authors).getStatus() == MemberStatus.ACTIVE && !visitor.test(entry)) return;
            }
            if (!page.isHasNext()) return;
            if (page.getNextCursor() == null || page.getNextCursor() <= 0 || (cursor > 0 && page.getNextCursor() >= cursor))
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid follow cursor from service");
            cursor = page.getNextCursor();
        }
    }

    private AuthorResponse author(String key, Map<String, AuthorResponse> authors) {
        return authors.computeIfAbsent(key, users::getAuthor);
    }

    private void requireActive(String key, Map<String, AuthorResponse> authors) {
        if (author(key, authors).getStatus() != MemberStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Withdrawn members cannot use follow operations");
    }
}
