package com.petfeed.contracts.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class SignupResponse {

    private String memberKey;
    private LocalDateTime createdAt;
}
