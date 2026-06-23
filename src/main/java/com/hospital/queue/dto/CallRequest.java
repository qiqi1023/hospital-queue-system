package com.hospital.queue.dto;
import jakarta.validation.constraints.NotBlank;
public record CallRequest(
	@NotBlank(message = "Department is required") String departmentCode,
	@NotBlank(message = "Counter is required") String counterName) {}
