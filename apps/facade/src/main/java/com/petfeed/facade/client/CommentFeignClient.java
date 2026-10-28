package com.petfeed.facade.client;

import com.petfeed.domain.dto.request.SaveCommentRequest;
import com.petfeed.domain.dto.response.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import static com.petfeed.domain.http.MyHttpHeaders.X_USER_KEY;

@FeignClient(name = "comment-service", url = "${services.comment-service.url}")
public interface CommentFeignClient {
    @DeleteMapping("/internal/v1/posts/{postId}/comments")
    void close(@PathVariable("postId") Long postId);

    @GetMapping("/internal/v1/comments/counts")
    java.util.Map<Long, Long> counts(@RequestParam("postIds") java.util.List<Long> postIds);
    @PostMapping("/api/v1/posts/{postId}/comments")
    CommentResponse create(@PathVariable("postId") Long postId, @RequestHeader(X_USER_KEY) String key, @RequestBody SaveCommentRequest request);

    @GetMapping("/api/v1/posts/{postId}/comments")
    CommentListResponse list(@PathVariable("postId") Long postId, @RequestParam("cursor") Long cursor);

    @PutMapping("/api/v1/posts/{postId}/comments/{id}")
    void update(@PathVariable("postId") Long postId, @PathVariable("id") Long id, @RequestHeader(X_USER_KEY) String key, @RequestBody SaveCommentRequest request);

    @DeleteMapping("/api/v1/posts/{postId}/comments/{id}")
    void delete(@PathVariable("postId") Long postId, @PathVariable("id") Long id, @RequestHeader(X_USER_KEY) String key);
}
