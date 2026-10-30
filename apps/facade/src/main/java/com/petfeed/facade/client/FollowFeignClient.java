package com.petfeed.facade.client;

import com.petfeed.domain.dto.response.FollowPage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.petfeed.domain.http.MyHttpHeaders.X_USER_KEY;

@FeignClient(name = "follow-service", url = "${services.follow-service.url}")
public interface FollowFeignClient {
    @PostMapping("/api/v1/following/{key}")
    void follow(@PathVariable("key") String key, @RequestHeader(X_USER_KEY) String requester);
    @DeleteMapping("/api/v1/following/{key}")
    void unfollow(@PathVariable("key") String key, @RequestHeader(X_USER_KEY) String requester);
    @GetMapping("/internal/v1/users/{key}/followers")
    FollowPage followers(@PathVariable("key") String key, @RequestParam("cursor") Long cursor);
    @GetMapping("/internal/v1/users/{key}/followings")
    FollowPage followings(@PathVariable("key") String key, @RequestParam("cursor") Long cursor);
    @GetMapping("/internal/v1/follows/targets")
    Set<String> targets(@RequestParam("requester") String requester, @RequestParam("keys") List<String> keys);
}
