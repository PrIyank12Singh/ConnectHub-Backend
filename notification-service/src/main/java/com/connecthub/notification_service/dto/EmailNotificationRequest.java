package com.ConnectHub.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailNotificationRequest {

    @NotBlank(message = "toEmail is required")
    @Email(message = "Invalid email address")
    private String toEmail;

    @NotBlank(message = "subject is required")
    private String subject;

    @NotBlank(message = "body is required")
    private String body;
}
