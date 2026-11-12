package com.petfeed.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class SaveCommentRequest {
    @NotBlank @Size(max = 1000)
    private String content;
}
