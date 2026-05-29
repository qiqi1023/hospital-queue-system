package com.hospital.queue.dto;

public record CurrentQueueResponse(
		String departmentCode,
		String departmentName,
		String currentQueueNumber,
		int waitingCount,
		int usedSlots,
		int dailyQuota) {
}
