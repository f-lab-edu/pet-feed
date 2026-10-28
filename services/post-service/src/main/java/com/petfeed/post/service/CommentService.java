package com.petfeed.post.service;

import com.petfeed.domain.dto.request.SaveCommentRequest;
import com.petfeed.domain.dto.response.*;
import com.petfeed.post.domain.*;
import com.petfeed.post.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private static final int PAGE_SIZE = 20;
    private final CommentRepository comments;
    private final PostRepository posts;

    @Transactional
    public CommentResponse create(Long postId, String key, SaveCommentRequest request) {
        return response(comments.save(Comment.create(requirePost(postId), key, request.getContent())));
    }

    public CommentListResponse list(Long postId, Long cursor) {
        requirePost(postId);
        if (cursor == null || cursor < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor must be nonnegative");
        var page = PageRequest.of(0, PAGE_SIZE + 1);
        var found = cursor == 0 ? comments.findByPostIdOrderByIdDesc(postId, page)
                : comments.findByPostIdAndIdLessThanOrderByIdDesc(postId, cursor, page);
        boolean hasNext = found.size() > PAGE_SIZE;
        var visible = found.stream().limit(PAGE_SIZE).map(this::response).toList();
        return CommentListResponse.of(visible, hasNext ? visible.getLast().getCommentId() : null, hasNext);
    }

    @Transactional
    public void update(Long postId, Long id, String key, SaveCommentRequest request) {
        requireComment(postId, id).update(key, request.getContent());
    }

    @Transactional
    public void delete(Long postId, Long id, String key) {
        var comment = requireComment(postId, id);
        comment.requireOwner(key);
        comments.delete(comment);
    }

    private Post requirePost(Long id) {
        return posts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private Comment requireComment(Long postId, Long id) {
        return comments.findByIdAndPostId(id, postId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    }

    private CommentResponse response(Comment comment) {
        return CommentResponse.of(comment.getId(), comment.getPost().getId(), comment.getUserKey(), comment.getContent(),
                comment.getCreatedAt(), comment.getUpdatedAt(), null);
    }
}
