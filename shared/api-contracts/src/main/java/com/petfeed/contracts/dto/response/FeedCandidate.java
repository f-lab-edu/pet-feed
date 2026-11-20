package com.petfeed.contracts.dto.response;

import java.time.LocalDateTime;

public record FeedCandidate(Long postId, String memberKey, LocalDateTime createdAt) {}
