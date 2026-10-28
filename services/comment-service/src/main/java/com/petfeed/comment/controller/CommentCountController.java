package com.petfeed.comment.controller;

import com.petfeed.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CommentCountController {
    private final CommentService comments;

    @DeleteMapping("/internal/v1/posts/{postId}/comments")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void close(@PathVariable("postId") Long postId) { comments.close(postId); }

    @GetMapping("/internal/v1/comments/counts")
    public Map<Long, Long> counts(@RequestParam("postIds") List<Long> postIds) {
        return comments.counts(postIds);
    }
}
