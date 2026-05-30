package com.hospital.queue.dto;

import com.hospital.queue.domain.QueueStatus;

public record CurrentQueueResponse(
		String departmentCode,
		String departmentName,
		String currentQueueNumber,
		String counterName,
		QueueStatus serviceStatus,
		int waitingCount,
		int usedSlots,
		int dailyQuota) {
}
