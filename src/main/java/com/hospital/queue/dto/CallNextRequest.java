package com.hospital.queue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CallNextRequest(
		@NotBlank(message = "Department code is required")
		String departmentCode,

		@NotBlank(message = "Counter name is required")
		@Size(max = 80, message = "Counter name must be 80 characters or fewer")
		String counterName) {
}
