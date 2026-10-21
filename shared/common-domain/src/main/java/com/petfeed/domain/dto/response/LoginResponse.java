package com.petfeed.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String accessTokenExpiresIn;
    private String grantType;
}
