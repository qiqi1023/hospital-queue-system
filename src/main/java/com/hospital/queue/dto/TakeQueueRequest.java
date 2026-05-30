package com.hospital.queue.dto;

import com.hospital.queue.domain.PriorityCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TakeQueueRequest(
		@NotBlank(message = "Full name is required")
		@Size(max = 120, message = "Full name must be 120 characters or fewer")
		String patientName,

		@NotBlank(message = "IC number is required")
		String icNumber,

		@NotBlank(message = "Phone number is required")
		@Pattern(regexp = "01[0-9]-[0-9]{7,8}", message = "Phone number must be a valid Malaysian mobile number, for example 012-3456789")
		@Size(max = 30, message = "Phone number must be 30 characters or fewer")
		String phoneNumber,

		@NotBlank(message = "Department code is required")
		String departmentCode,

		@NotBlank(message = "Visit reason is required")
		@Size(max = 500, message = "Visit reason must be 500 characters or fewer")
		String visitReason,

		@NotNull(message = "Priority category is required")
		PriorityCategory priorityCategory) {
}
