package com.petfeed.contracts.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class AuthorResponse {
    private AuthorStatus status;
    private String displayName;
}
