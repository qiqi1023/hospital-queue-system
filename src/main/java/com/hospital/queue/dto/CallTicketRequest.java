package com.hospital.queue.dto;

import jakarta.validation.constraints.NotBlank;

public record CallTicketRequest(
	@NotBlank(message = "Counter is required") String counterName) {}
