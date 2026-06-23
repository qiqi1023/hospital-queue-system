package com.hospital.queue.dto;

import com.hospital.queue.model.IdentityType;
import jakarta.validation.constraints.*;

public record TakeQueueRequest(
	@NotNull(message = "Identity type is required") IdentityType identityType,
	@NotBlank(message = "Identity number is required") String identityNumber,
	@NotBlank(message = "Country code is required") String phoneCountryCode,
	@NotBlank(message = "Phone number is required") String phoneNumber,
	@NotBlank(message = "Department is required") String departmentCode) {}
