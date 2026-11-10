package com.petfeed.media.controller;

import com.petfeed.media.service.*;
import com.petfeed.domain.dto.response.MediaResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.util.List;
import static com.petfeed.domain.http.MyHttpHeaders.X_USER_KEY;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/media")
public class MediaController {
    private final MediaService media;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MediaResponse upload(@RequestHeader(X_USER_KEY) String owner, HttpServletRequest request) throws IOException {
        byte[] body = request.getInputStream().readNBytes(MediaValidator.MAX_BYTES + 1);
        return media.upload(owner, request.getContentType(), body);
    }

    @PostMapping("/validate")
    public List<MediaResponse> validate(@RequestHeader(X_USER_KEY) String owner, @RequestBody List<String> ids) {
        return media.validateOwnership(owner, ids);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(@PathVariable("id") String id) {
        var file = media.get(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getSize()).header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore()).body(media.resource(file));
    }
}
