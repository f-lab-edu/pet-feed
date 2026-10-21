package com.petfeed.facade.client;

import com.petfeed.domain.dto.response.PostListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "post-service", url = "${services.post-service.url}")
public interface PostFeignClient {

    @GetMapping("/api/v1/posts/count/{user-key}")
    long getPostCount(@PathVariable("user-key") String userKey);

    @GetMapping("/api/v1/users/{user-key}/posts")
    PostListResponse getPostsByUser(@PathVariable("user-key") String userKey,
                                    @RequestParam(defaultValue = "0") Long cursor);
}
