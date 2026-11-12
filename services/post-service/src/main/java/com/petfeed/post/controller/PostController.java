package com.petfeed.post.controller;

import com.petfeed.contracts.dto.request.SavePostRequest;
import com.petfeed.contracts.dto.response.*;
import com.petfeed.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import static com.petfeed.contracts.http.MyHttpHeaders.X_MEMBER_KEY;

@RestController
@RequiredArgsConstructor
public class PostController {
    private final PostService posts;

    @PostMapping("/api/v1/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailResponse create(@RequestHeader(X_MEMBER_KEY) String key, @Valid @RequestBody SavePostRequest request) {
        return posts.create(key, request);
    }

    @GetMapping("/api/v1/posts/{id}")
    public PostDetailResponse get(@PathVariable("id") Long id) { return posts.get(id); }

    @PutMapping("/api/v1/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable("id") Long id, @RequestHeader(X_MEMBER_KEY) String key,
                       @Valid @RequestBody SavePostRequest request) { posts.update(id, key, request); }

    @DeleteMapping("/api/v1/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id, @RequestHeader(X_MEMBER_KEY) String key) { posts.delete(id, key); }

    @GetMapping("/api/v1/posts/count/{member-key}")
    public long count(@PathVariable("member-key") String key) { return posts.count(key); }

    @GetMapping("/api/v1/members/{member-key}/posts")
    public PostListResponse list(@PathVariable("member-key") String key,
                                @RequestParam(name = "cursor", defaultValue = "0") Long cursor) {
        return posts.list(key, cursor);
    }
}
