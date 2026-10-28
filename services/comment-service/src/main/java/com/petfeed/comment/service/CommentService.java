package com.petfeed.comment.service;

import com.petfeed.domain.dto.request.SaveCommentRequest;
import com.petfeed.domain.dto.response.*;
import com.petfeed.comment.domain.*;
import com.petfeed.comment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private static final int PAGE_SIZE = 20;
    private final CommentRepository comments;
    private final CommentThreadRepository threads;
    private final CommentThreadInitializer initializer;

    @Transactional
    public CommentResponse create(Long postId, String key, SaveCommentRequest request) {
        var thread = lockThread(postId);
        if (thread.isClosed())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post comments are closed");
        return response(comments.save(Comment.create(postId, key, request.getContent())));
    }

    public CommentListResponse list(Long postId, Long cursor) {
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

    @Transactional
    public void close(Long postId) {
        lockThread(postId).close();
        comments.deleteByPostId(postId);
    }

    public Map<Long, Long> counts(List<Long> postIds) {
        if (postIds.size() > 20 || postIds.stream().anyMatch(id -> id == null || id <= 0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most 20 positive post IDs are allowed");
        return comments.countForPosts(postIds).stream()
                .collect(Collectors.toMap(CommentRepository.PostCount::getPostId, CommentRepository.PostCount::getTotal));
    }

    private CommentThread lockThread(Long postId) {
        if (postId == null || postId <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Post ID must be positive");
        initializer.ensure(postId);
        return threads.lockByPostId(postId).orElseThrow();
    }

    private Comment requireComment(Long postId, Long id) {
        return comments.findByIdAndPostId(id, postId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    }

    private CommentResponse response(Comment comment) {
        return CommentResponse.of(comment.getId(), comment.getPostId(), comment.getUserKey(), comment.getContent(),
                comment.getCreatedAt(), comment.getUpdatedAt(), null);
    }
}
