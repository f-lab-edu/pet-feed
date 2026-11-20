package com.petfeed.contracts.dto.response;

public record FeedItem(PostDetailResponse post, long commentCount, String source) {}
