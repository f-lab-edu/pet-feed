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

    @GetMapping("/api/v1/posts/count/{user-key}")
    Long getPostCount(@PathVariable("user-key") String memberKey);

    @GetMapping("/api/v1/users/{user-key}/posts")
    PostListResponse getPostsByMember(@PathVariable("user-key") String memberKey,
                                    @RequestParam(defaultValue = "0") Long cursor);
}
