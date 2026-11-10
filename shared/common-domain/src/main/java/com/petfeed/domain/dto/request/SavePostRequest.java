package com.petfeed.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class SavePostRequest {
    @NotBlank
    @Size(max = 5000)
    private String content;
    @Size(max = 2048)
    private String thumbnailUrl;
    @Size(max = 6)
    private java.util.List<@jakarta.validation.constraints.NotNull
            @jakarta.validation.constraints.Pattern(regexp = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}") String> mediaIds;

    public static SavePostRequest of(String content, String thumbnailUrl) {
        return new SavePostRequest(content, thumbnailUrl, null);
    }
}
