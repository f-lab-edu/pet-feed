package com.petfeed.facade.controller;

import com.petfeed.contracts.dto.request.SavePostRequest;
import com.petfeed.contracts.dto.response.*;
import com.petfeed.facade.client.PostFeignClient;
import com.petfeed.facade.client.MemberFeignClient;
import com.petfeed.facade.client.MediaFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class PostFacade {
    private final PostFeignClient posts;
    private final MemberFeignClient members;
    private final MediaFeignClient media;

    public PostDetailResponse create(String key, SavePostRequest request) {
        var author = requireActive(key);
        validateMedia(key, request);
        return posts.create(key, request).withAuthor(author);
    }

    public PostDetailResponse get(Long id) {
        var post = posts.get(id);
        return post.withAuthor(members.getAuthor(post.getMemberKey()));
    }

    public void update(Long id, String key, SavePostRequest request) {
        requireActive(key);
        validateMedia(key, request);
        posts.update(id, key, request);
    }

    public void delete(Long id, String key) {
        requireActive(key);
        posts.delete(id, key);
    }

    private AuthorResponse requireActive(String key) {
        var author = members.getAuthor(key);
        if (author.getStatus() != MemberStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Withdrawn members cannot change posts");
        return author;
    }

    private void validateMedia(String key, SavePostRequest request) {
        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty())
            media.validate(key, request.getMediaIds());
    }
}
