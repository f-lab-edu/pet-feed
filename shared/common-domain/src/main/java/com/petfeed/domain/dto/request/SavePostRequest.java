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
}
