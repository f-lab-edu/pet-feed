package com.petfeed.facade.client;

import com.petfeed.domain.dto.response.PostListResponse;
import com.petfeed.domain.dto.response.PostDetailResponse;
import com.petfeed.domain.dto.request.SavePostRequest;
import org.springframework.web.bind.annotation.*;
import static com.petfeed.domain.http.MyHttpHeaders.X_USER_KEY;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "post-service", url = "${services.post-service.url}")
public interface PostFeignClient {

    @PostMapping("/api/v1/posts")
    PostDetailResponse create(
            @RequestHeader(X_USER_KEY) String key,
            @RequestBody SavePostRequest request);

    @GetMapping("/api/v1/posts/{id}")
    PostDetailResponse get(@PathVariable("id") Long id);

    @PutMapping("/api/v1/posts/{id}")
    void update(@PathVariable("id") Long id,
            @RequestHeader(X_USER_KEY) String key,
            @RequestBody SavePostRequest request);

    @DeleteMapping("/api/v1/posts/{id}")
    void delete(@PathVariable("id") Long id,
            @RequestHeader(X_USER_KEY) String key);

    @GetMapping("/api/v1/posts/count/{user-key}")
    Long getPostCount(@PathVariable("user-key") String userKey);

    @GetMapping("/api/v1/users/{user-key}/posts")
    PostListResponse getPostsByUser(@PathVariable("user-key") String userKey,
                                    @RequestParam(defaultValue = "0") Long cursor);
}
