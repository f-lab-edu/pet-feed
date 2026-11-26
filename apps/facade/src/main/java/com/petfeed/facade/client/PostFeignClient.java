package com.petfeed.facade.client;

import com.petfeed.contracts.http.ApiPaths;

import com.petfeed.contracts.dto.response.PostListResponse;
import com.petfeed.contracts.dto.response.PostDetailResponse;
import com.petfeed.contracts.dto.request.SavePostRequest;
import org.springframework.web.bind.annotation.*;
import static com.petfeed.contracts.http.MyHttpHeaders.X_MEMBER_KEY;
import org.springframework.cloud.openfeign.FeignClient;

import java.util.List;

@FeignClient(name = "post-service", url = "${services.post-service.url}")
public interface PostFeignClient {
    @GetMapping(ApiPaths.INTERNAL_V1 + "/feed/candidates")
    java.util.List<com.petfeed.contracts.dto.response.FeedCandidate> feedCandidates(@RequestParam("requester") String requester);

    @PostMapping(ApiPaths.INTERNAL_V1 + "/feed/posts")
    java.util.List<PostDetailResponse> feedDetails(@RequestBody List<Long> ids);

    @PostMapping(ApiPaths.API_V1 + "/posts")
    PostDetailResponse create(
            @RequestHeader(X_MEMBER_KEY) String key,
            @RequestBody SavePostRequest request);

    @GetMapping(ApiPaths.API_V1 + "/posts/{id}")
    PostDetailResponse get(@PathVariable("id") Long id);

    @PutMapping(ApiPaths.API_V1 + "/posts/{id}")
    void update(@PathVariable("id") Long id,
            @RequestHeader(X_MEMBER_KEY) String key,
            @RequestBody SavePostRequest request);

    @DeleteMapping(ApiPaths.API_V1 + "/posts/{id}")
    void delete(@PathVariable("id") Long id,
            @RequestHeader(X_MEMBER_KEY) String key);

    @GetMapping(ApiPaths.API_V1 + "/posts/count/{member-key}")
    Long getPostCount(@PathVariable("member-key") String memberKey);

    @GetMapping(ApiPaths.API_V1 + "/members/{member-key}/posts")
    PostListResponse getPostsByMember(@PathVariable("member-key") String memberKey,
                                    @RequestParam(defaultValue = "0") Long cursor);
}
