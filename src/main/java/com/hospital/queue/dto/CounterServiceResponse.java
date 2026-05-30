package com.hospital.queue.dto;

import com.hospital.queue.domain.QueueStatus;
import java.time.LocalDateTime;

public record CounterServiceResponse(
		String counterName,
		String queueNumber,
		String patientName,
		String departmentCode,
		String departmentName,
		QueueStatus status,
		LocalDateTime calledAt) {
}
