package com.hospital.queue.dto;

import com.hospital.queue.domain.PriorityCategory;
import com.hospital.queue.domain.QueueStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record QueueTicketResponse(
		String queueNumber,
		String patientName,
		String icNumber,
		String phoneNumber,
		String departmentCode,
		String departmentName,
		String visitReason,
		PriorityCategory priorityCategory,
		QueueStatus status,
		String counterName,
		int peopleAhead,
		Integer estimatedWaitMinutes,
		LocalDate queueDate,
		LocalDateTime createdAt,
		LocalDateTime calledAt,
		LocalDateTime completedAt) {
}
