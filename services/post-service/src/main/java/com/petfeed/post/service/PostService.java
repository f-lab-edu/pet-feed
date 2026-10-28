package com.petfeed.post.service;

import com.petfeed.domain.dto.request.SavePostRequest;
import com.petfeed.domain.dto.response.*;
import com.petfeed.post.domain.Post;
import com.petfeed.post.repository.PostRepository;
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

    @Transactional
    public PostDetailResponse create(String userKey, SavePostRequest request) {
        return detail(posts.save(Post.create(userKey, request.getContent(), request.getThumbnailUrl())));
    }

    public PostDetailResponse get(Long id) { return detail(requirePost(id)); }

    @Transactional
    public void update(Long id, String requesterKey, SavePostRequest request) {
        requirePost(id).update(requesterKey, request.getContent(), request.getThumbnailUrl());
    }

    @Transactional
    public void delete(Long id, String requesterKey) {
        Post post = requirePost(id);
        post.requireOwner(requesterKey);
        posts.delete(post);
    }

    public long count(String userKey) { return posts.countByUserKey(userKey); }

    public PostListResponse list(String userKey, Long cursor) {
        if (cursor == null || cursor < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor must be nonnegative");
        var page = PageRequest.of(0, PAGE_SIZE + 1);
        var found = cursor == 0 ? posts.findByUserKeyOrderByIdDesc(userKey, page)
                : posts.findByUserKeyAndIdLessThanOrderByIdDesc(userKey, cursor, page);
        boolean hasNext = found.size() > PAGE_SIZE;
        var visible = found.stream().limit(PAGE_SIZE).map(post -> PostSummary.of(post.getId(),
                post.getThumbnailUrl(), 0, 0, post.getCreatedAt(), post.getUpdatedAt(), null)).toList();
        return PostListResponse.of(visible, hasNext ? visible.get(visible.size() - 1).getPostId() : null, hasNext);
    }

    private Post requirePost(Long id) {
        return posts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private PostDetailResponse detail(Post post) {
        return PostDetailResponse.of(post.getId(), post.getUserKey(), post.getContent(), post.getThumbnailUrl(),
                post.getCreatedAt(), post.getUpdatedAt(), null);
    }
}
