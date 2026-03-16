package com.petfeed.facade.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "post-service", url = "${services.post-service.url}")
public interface PostFeignClient {

    @GetMapping("/api/v1/posts/count/{user-key}")
    long getPostCount(@PathVariable("user-key") String userKey);
}
