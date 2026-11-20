package com.petfeed.contracts.dto.response;

import java.util.List;

public record FeedResponse(List<FeedItem> items, String nextCursor, boolean hasNext) {}
