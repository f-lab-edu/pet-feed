package com.petfeed.facade.client;

import com.petfeed.contracts.dto.response.MediaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.petfeed.contracts.http.MyHttpHeaders.X_MEMBER_KEY;

@FeignClient(name = "media-service", url = "${services.media-service.url:http://localhost:8086}")
public interface MediaFeignClient {
    @PostMapping("/internal/v1/media/validate")
    List<MediaResponse> validate(@RequestHeader(X_MEMBER_KEY) String owner, @RequestBody List<String> ids);
}
