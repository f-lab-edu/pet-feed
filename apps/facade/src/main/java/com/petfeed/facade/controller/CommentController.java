package com.petfeed.facade.controller;

import com.petfeed.domain.dto.request.SaveCommentRequest;
import com.petfeed.domain.dto.response.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import static com.petfeed.domain.http.MyHttpHeaders.X_USER_KEY;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts/{postId}/comments")
public class CommentController {
    private final CommentFacade comments;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@PathVariable("postId") Long postId, @RequestHeader(X_USER_KEY) String key,
            @Valid @RequestBody SaveCommentRequest request) { return comments.create(postId, key, request); }

    @GetMapping
    public CommentListResponse list(@PathVariable("postId") Long postId,
            @RequestParam(name = "cursor", defaultValue = "0") Long cursor) { return comments.list(postId, cursor); }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable("postId") Long postId, @PathVariable("id") Long id,
            @RequestHeader(X_USER_KEY) String key, @Valid @RequestBody SaveCommentRequest request) {
        comments.update(postId, id, key, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("postId") Long postId, @PathVariable("id") Long id,
            @RequestHeader(X_USER_KEY) String key) { comments.delete(postId, id, key); }
}
