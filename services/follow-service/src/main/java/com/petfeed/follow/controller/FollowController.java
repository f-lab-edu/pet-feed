package com.petfeed.follow.controller;

import com.petfeed.follow.service.FollowService;
import com.petfeed.domain.dto.response.FollowPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.petfeed.domain.http.MyHttpHeaders.X_USER_KEY;

@RestController
@RequiredArgsConstructor
public class FollowController {
    private final FollowService follows;

    @PostMapping("/api/v1/following/{key}")
    public void follow(@PathVariable("key") String key, @RequestHeader(X_USER_KEY) String requester) { follows.follow(requester, key); }
    @DeleteMapping("/api/v1/following/{key}")
    public void unfollow(@PathVariable("key") String key, @RequestHeader(X_USER_KEY) String requester) { follows.unfollow(requester, key); }
    @GetMapping("/internal/v1/users/{key}/followers")
    public FollowPage followers(@PathVariable("key") String key, @RequestParam(name = "cursor", defaultValue = "0") Long cursor) {
        return follows.list(key, cursor, true);
    }
    @GetMapping("/internal/v1/users/{key}/followings")
    public FollowPage followings(@PathVariable("key") String key, @RequestParam(name = "cursor", defaultValue = "0") Long cursor) {
        return follows.list(key, cursor, false);
    }
    @GetMapping("/internal/v1/follows/targets")
    public Set<String> targets(@RequestParam("requester") String requester, @RequestParam("keys") List<String> keys) {
        return follows.targets(requester, keys);
    }
}
