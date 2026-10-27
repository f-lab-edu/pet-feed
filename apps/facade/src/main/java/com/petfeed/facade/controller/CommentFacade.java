package com.petfeed.facade.controller;

import com.petfeed.domain.dto.request.SaveCommentRequest;
import com.petfeed.domain.dto.response.*;
import com.petfeed.facade.client.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class CommentFacade {
    private final CommentFeignClient comments;
    private final UserFeignClient users;

    public CommentResponse create(Long postId, String key, SaveCommentRequest request) {
        var author = requireActive(key);
        return comments.create(postId, key, request).withAuthor(author);
    }

    public CommentListResponse list(Long postId, Long cursor) {
        var page = comments.list(postId, cursor);
        var authors = new HashMap<String, AuthorResponse>();
        var enriched = page.getComments().stream().map(comment -> comment.withAuthor(
                authors.computeIfAbsent(comment.getUserKey(), users::getAuthor))).toList();
        return CommentListResponse.of(enriched, page.getNextCursor(), page.isHasNext());
    }

    public void update(Long postId, Long id, String key, SaveCommentRequest request) {
        requireActive(key);
        comments.update(postId, id, key, request);
    }

    public void delete(Long postId, Long id, String key) {
        requireActive(key);
        comments.delete(postId, id, key);
    }

    private AuthorResponse requireActive(String key) {
        var author = users.getAuthor(key);
        if (author.getStatus() != MemberStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Withdrawn members cannot change comments");
        return author;
    }
}
