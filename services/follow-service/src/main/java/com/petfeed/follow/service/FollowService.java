package com.petfeed.follow.service;

import com.petfeed.follow.domain.FollowRelation;
import com.petfeed.follow.repository.FollowRepository;
import com.petfeed.domain.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {
    private final FollowRepository follows;

    @Transactional
    public void follow(String requester, String target) {
        var relation = FollowRelation.create(requester, target);
        if (follows.existsByFollowerKeyAndFollowingKey(requester, target))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already following");
        follows.saveAndFlush(relation);
    }

    @Transactional
    public void unfollow(String requester, String target) {
        FollowRelation.validate(requester, target);
        follows.deleteByFollowerKeyAndFollowingKey(requester, target);
    }

    public FollowPage list(String key, Long cursor, boolean followers) {
        if (key == null || key.isBlank() || cursor == null || cursor < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid key or cursor");
        var page = PageRequest.of(0, 21);
        var found = followers ? follows.followers(key, cursor, page) : follows.followings(key, cursor, page);
        var visible = found.stream().limit(20).map(f -> FollowEntry.of(f.getId(),
                followers ? f.getFollowerKey() : f.getFollowingKey())).toList();
        boolean hasNext = found.size() > 20;
        return FollowPage.of(visible, hasNext ? visible.getLast().getFollowId() : null, hasNext);
    }

    public Set<String> targets(String requester, List<String> targets) {
        if (requester == null || requester.isBlank() || targets.size() > 20
                || targets.stream().anyMatch(key -> key == null || key.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most 20 member keys are allowed");
        return targets.isEmpty() ? Set.of() : follows.targets(requester, targets);
    }
}
