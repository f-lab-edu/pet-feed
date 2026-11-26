package com.petfeed.facade.controller;

import com.petfeed.contracts.http.ApiPaths;

import com.petfeed.contracts.dto.response.FeedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import static com.petfeed.contracts.http.MyHttpHeaders.X_MEMBER_KEY;

@RestController
@RequiredArgsConstructor
public class FeedController {
    private final FeedFacade feed;

    @GetMapping(ApiPaths.API_V1 + "/feed")
    public FeedResponse get(@RequestHeader(X_MEMBER_KEY) String requester,
                            @RequestParam(name = "cursor", required = false) String cursor) {
        return feed.get(requester, cursor);
    }
}
