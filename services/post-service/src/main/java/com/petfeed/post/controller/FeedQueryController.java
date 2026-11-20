package com.petfeed.post.controller;

import com.petfeed.contracts.http.ApiPaths;

import com.petfeed.contracts.dto.response.FeedCandidate;
import com.petfeed.contracts.dto.response.PostDetailResponse;
import com.petfeed.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Transactional(readOnly = true)
@RequestMapping(ApiPaths.INTERNAL_V1 + "/feed")
public class FeedQueryController {
    private final PostRepository posts;

    @GetMapping("/candidates")
    public List<FeedCandidate> candidates(@RequestParam("requester") String requester) {
        if (requester.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return posts.findByDeletedFalseAndMemberKeyNotAndCreatedAtGreaterThanEqualOrderByIdDesc(
                requester, LocalDateTime.now(java.time.Clock.systemDefaultZone()).minusDays(30), PageRequest.of(0, 500))
                .stream().map(p -> new FeedCandidate(p.getId(), p.getMemberKey(), p.getCreatedAt())).toList();
    }

    @PostMapping("/posts")
    public List<PostDetailResponse> details(@RequestBody List<Long> ids) {
        if (ids.size() > 10 || ids.stream().anyMatch(id -> id == null || id <= 0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most 10 positive post IDs");
        if (ids.isEmpty()) return List.of();
        return posts.findByIdInAndDeletedFalse(ids).stream().map(p -> PostDetailResponse.of(
                p.getId(), p.getMemberKey(), p.getContent(), p.getThumbnailUrl(), p.getCreatedAt(), p.getUpdatedAt(),
                null, List.copyOf(p.getMediaIds()))).toList();
    }
}
