package com.hospital.queue.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
		@NotBlank(message = "Admin ID is required") String username,
		@NotBlank(message = "Password is required") String password) {}
