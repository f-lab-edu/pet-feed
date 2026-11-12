package com.petfeed.facade.controller;

import com.petfeed.facade.client.*;
import com.petfeed.contracts.dto.response.MemberStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import static com.petfeed.contracts.http.MyHttpHeaders.X_MEMBER_KEY;

@RestController
public class MediaController {
    private final MemberFeignClient members;
    private final PostFeignClient posts;
    private final RestClient client;
    private static final int MAX_BYTES = 50 * 1024 * 1024;

    public MediaController(MemberFeignClient members, PostFeignClient posts, RestClient.Builder builder,
            @Value("${services.media-service.url:http://localhost:8086}") String url) {
        this.members = members;
        this.posts = posts;
        this.client = builder.baseUrl(url).build();
    }

    @PostMapping("/api/v1/media")
    public ResponseEntity<byte[]> upload(@RequestHeader(X_MEMBER_KEY) String owner, HttpServletRequest request) throws IOException {
        if (members.getAuthor(owner).getStatus() != MemberStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        byte[] body = request.getInputStream().readNBytes(MAX_BYTES + 1);
        if (body.length > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        try {
            return client.post().uri("/internal/v1/media").header(X_MEMBER_KEY, owner)
                    .contentType(MediaType.parseMediaType(request.getContentType() == null ? "application/octet-stream" : request.getContentType()))
                    .body(body).retrieve().toEntity(byte[].class);
        } catch (RestClientResponseException failure) {
            return ResponseEntity.status(failure.getStatusCode()).body(failure.getResponseBodyAsByteArray());
        }
    }

    @GetMapping("/api/v1/posts/{postId}/media/{mediaId}")
    public ResponseEntity<byte[]> content(@PathVariable("postId") Long postId, @PathVariable("mediaId") String mediaId,
            @RequestHeader(value = "Range", required = false) String range) {
        var post = posts.get(postId);
        if (post.getMediaIds() == null || !post.getMediaIds().contains(mediaId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        try {
            return client.get().uri("/internal/v1/media/{id}/content", mediaId)
                    .headers(headers -> { if (range != null) headers.set("Range", range); })
                    .retrieve().toEntity(byte[].class);
        } catch (RestClientResponseException failure) {
            return ResponseEntity.status(failure.getStatusCode()).headers(failure.getResponseHeaders())
                    .body(failure.getResponseBodyAsByteArray());
        }
    }
}
