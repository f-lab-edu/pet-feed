package com.petfeed.facade.client;

import com.petfeed.contracts.http.ApiPaths;

import com.petfeed.contracts.dto.response.FollowPage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.petfeed.contracts.http.MyHttpHeaders.X_MEMBER_KEY;

@FeignClient(name = "follow-service", url = "${services.follow-service.url}")
public interface FollowFeignClient {
    @PostMapping(ApiPaths.API_V1 + "/following/{key}")
    void follow(@PathVariable("key") String key, @RequestHeader(X_MEMBER_KEY) String requester);
    @DeleteMapping(ApiPaths.API_V1 + "/following/{key}")
    void unfollow(@PathVariable("key") String key, @RequestHeader(X_MEMBER_KEY) String requester);
    @GetMapping(ApiPaths.INTERNAL_V1 + "/members/{key}/followers")
    FollowPage followers(@PathVariable("key") String key, @RequestParam("cursor") Long cursor);
    @GetMapping(ApiPaths.INTERNAL_V1 + "/members/{key}/followings")
    FollowPage followings(@PathVariable("key") String key, @RequestParam("cursor") Long cursor);
    @GetMapping(ApiPaths.INTERNAL_V1 + "/follows/targets")
    Set<String> targets(@RequestParam("requester") String requester, @RequestParam("keys") List<String> keys);
}
