package com.petfeed.post.service;

import com.petfeed.domain.dto.request.SavePostRequest;
import com.petfeed.domain.dto.response.*;
import com.petfeed.post.domain.Post;
import com.petfeed.post.domain.CommentCleanup;
import com.petfeed.post.repository.PostRepository;
import com.petfeed.post.repository.CommentCleanupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private static final int PAGE_SIZE = 20;
    private final PostRepository posts;
    private final CommentCleanupRepository cleanup;

    @Transactional
    public PostDetailResponse create(String userKey, SavePostRequest request) {
        var post = Post.create(userKey, request.getContent(), request.getThumbnailUrl());
        post.replaceMedia(request.getMediaIds());
        return detail(posts.save(post));
    }

    public PostDetailResponse get(Long id) { return detail(requirePost(id)); }

    @Transactional
    public void update(Long id, String requesterKey, SavePostRequest request) {
        var post = requirePost(id);
        post.update(requesterKey, request.getContent(), request.getThumbnailUrl());
        post.replaceMedia(request.getMediaIds());
    }

    @Transactional
    public void delete(Long id, String requesterKey) {
        Post post = posts.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        post.requireOwner(requesterKey);
        if (post.isDeleted()) return;
        post.markDeleted();
        cleanup.save(new CommentCleanup(id));
    }

    public long count(String userKey) { return posts.countByUserKeyAndDeletedFalse(userKey); }

    public PostListResponse list(String userKey, Long cursor) {
        if (cursor == null || cursor < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor must be nonnegative");
        var page = PageRequest.of(0, PAGE_SIZE + 1);
        var found = cursor == 0 ? posts.findByUserKeyAndDeletedFalseOrderByIdDesc(userKey, page)
                : posts.findByUserKeyAndDeletedFalseAndIdLessThanOrderByIdDesc(userKey, cursor, page);
        boolean hasNext = found.size() > PAGE_SIZE;
        var visible = found.stream().limit(PAGE_SIZE).map(post -> PostSummary.of(post.getId(),
                post.getThumbnailUrl(), 0, 0, post.getCreatedAt(), post.getUpdatedAt(), null)).toList();
        return PostListResponse.of(visible, hasNext ? visible.get(visible.size() - 1).getPostId() : null, hasNext);
    }

    private Post requirePost(Long id) {
        return posts.findById(id).filter(post -> !post.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private PostDetailResponse detail(Post post) {
        return PostDetailResponse.of(post.getId(), post.getUserKey(), post.getContent(), post.getThumbnailUrl(),
                post.getCreatedAt(), post.getUpdatedAt(), null, java.util.List.copyOf(post.getMediaIds()));
    }
}
