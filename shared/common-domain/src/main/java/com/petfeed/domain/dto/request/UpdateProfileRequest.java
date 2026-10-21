package com.petfeed.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class UpdateProfileRequest {

    private String bio;
    @Email
    @Pattern(regexp = ".*\\S.*", message = "email must not be blank")
    private String email;
}
