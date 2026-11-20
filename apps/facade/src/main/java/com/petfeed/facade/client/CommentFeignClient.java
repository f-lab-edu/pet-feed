package com.petfeed.facade.client;

import com.petfeed.contracts.http.ApiPaths;

import com.petfeed.contracts.dto.request.SaveCommentRequest;
import com.petfeed.contracts.dto.response.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import static com.petfeed.contracts.http.MyHttpHeaders.X_MEMBER_KEY;

@FeignClient(name = "comment-service", url = "${services.comment-service.url}")
public interface CommentFeignClient {
    @DeleteMapping(ApiPaths.INTERNAL_V1 + "/posts/{postId}/comments")
    void close(@PathVariable("postId") Long postId);

    @GetMapping(ApiPaths.INTERNAL_V1 + "/comments/counts")
    java.util.Map<Long, Long> counts(@RequestParam("postIds") java.util.List<Long> postIds);
    @PostMapping(ApiPaths.API_V1 + "/posts/{postId}/comments")
    CommentResponse create(@PathVariable("postId") Long postId, @RequestHeader(X_MEMBER_KEY) String key, @RequestBody SaveCommentRequest request);

    @GetMapping(ApiPaths.API_V1 + "/posts/{postId}/comments")
    CommentListResponse list(@PathVariable("postId") Long postId, @RequestParam("cursor") Long cursor);

    @PutMapping(ApiPaths.API_V1 + "/posts/{postId}/comments/{id}")
    void update(@PathVariable("postId") Long postId, @PathVariable("id") Long id, @RequestHeader(X_MEMBER_KEY) String key, @RequestBody SaveCommentRequest request);

    @DeleteMapping(ApiPaths.API_V1 + "/posts/{postId}/comments/{id}")
    void delete(@PathVariable("postId") Long postId, @PathVariable("id") Long id, @RequestHeader(X_MEMBER_KEY) String key);
}
