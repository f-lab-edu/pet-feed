package com.petfeed.contracts.dto.response;

import java.time.Instant;

public record FeedCandidate(Long postId, String memberKey, Instant createdAt) {}
