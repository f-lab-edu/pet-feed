package com.petfeed.facade.controller;

import com.petfeed.domain.dto.request.SavePostRequest;
import com.petfeed.domain.dto.response.PostDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import static com.petfeed.domain.http.MyHttpHeaders.X_USER_KEY;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostFacade posts;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailResponse create(@RequestHeader(X_USER_KEY) String key, @Valid @RequestBody SavePostRequest request) {
        return posts.create(key, request);
    }

    @GetMapping("/{id}")
    public PostDetailResponse get(@PathVariable("id") Long id, @RequestHeader(X_USER_KEY) String key) {
        return posts.get(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable("id") Long id, @RequestHeader(X_USER_KEY) String key,
                       @Valid @RequestBody SavePostRequest request) { posts.update(id, key, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id, @RequestHeader(X_USER_KEY) String key) { posts.delete(id, key); }
}
