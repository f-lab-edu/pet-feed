package com.petfeed.facade.controller;

import com.petfeed.domain.dto.request.SavePostRequest;
import com.petfeed.domain.dto.response.*;
import com.petfeed.facade.client.PostFeignClient;
import com.petfeed.facade.client.UserFeignClient;
import com.petfeed.facade.client.CommentFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class PostFacade {
    private final PostFeignClient posts;
    private final UserFeignClient users;
    private final CommentFeignClient comments;

    public PostDetailResponse create(String key, SavePostRequest request) {
        var author = requireActive(key);
        return posts.create(key, request).withAuthor(author);
    }

    public PostDetailResponse get(Long id) {
        var post = posts.get(id);
        return post.withAuthor(users.getAuthor(post.getUserKey()));
    }

    public void update(Long id, String key, SavePostRequest request) {
        requireActive(key);
        posts.update(id, key, request);
    }

    public void delete(Long id, String key) {
        requireActive(key);
        var post = posts.get(id);
        if (!key.equals(post.getUserKey()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can delete this post");
        comments.close(id);
        posts.delete(id, key);
    }

    private AuthorResponse requireActive(String key) {
        var author = users.getAuthor(key);
        if (author.getStatus() != MemberStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Withdrawn members cannot change posts");
        return author;
    }
}
