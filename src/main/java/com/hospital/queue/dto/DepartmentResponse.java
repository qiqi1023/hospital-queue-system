package com.hospital.queue.dto;

public record DepartmentResponse(
		String code,
		String name,
		int dailyQuota) {
}
