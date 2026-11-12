package com.petfeed.facade.client;

import com.petfeed.contracts.dto.response.PostListResponse;
import com.petfeed.contracts.dto.response.PostDetailResponse;
import com.petfeed.contracts.dto.request.SavePostRequest;
import org.springframework.web.bind.annotation.*;
import static com.petfeed.contracts.http.MyHttpHeaders.X_MEMBER_KEY;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "post-service", url = "${services.post-service.url}")
public interface PostFeignClient {

    @PostMapping("/api/v1/posts")
    PostDetailResponse create(
            @RequestHeader(X_MEMBER_KEY) String key,
            @RequestBody SavePostRequest request);

    @GetMapping("/api/v1/posts/{id}")
    PostDetailResponse get(@PathVariable("id") Long id);

    @PutMapping("/api/v1/posts/{id}")
    void update(@PathVariable("id") Long id,
            @RequestHeader(X_MEMBER_KEY) String key,
            @RequestBody SavePostRequest request);

    @DeleteMapping("/api/v1/posts/{id}")
    void delete(@PathVariable("id") Long id,
            @RequestHeader(X_MEMBER_KEY) String key);

    @GetMapping("/api/v1/posts/count/{member-key}")
    Long getPostCount(@PathVariable("member-key") String memberKey);

    @GetMapping("/api/v1/members/{member-key}/posts")
    PostListResponse getPostsByMember(@PathVariable("member-key") String memberKey,
                                    @RequestParam(defaultValue = "0") Long cursor);
}
