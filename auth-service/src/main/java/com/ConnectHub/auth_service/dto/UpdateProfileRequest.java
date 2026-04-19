package com.ConnectHub.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @NotBlank
    @Size(min = 2, max = 120)
    private String fullName;

    @Size(max = 500)
    private String avatarUrl;

    @Size(max = 400)
    private String bio;
}
