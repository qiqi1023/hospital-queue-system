package com.hospital.queue.dto;

import com.hospital.queue.model.QueueStatus;
import java.time.*;

public record QueueResponse(String queueNumber, String departmentCode, String departmentName,
	QueueStatus status, String counterName, long peopleAhead, LocalDate queueDate,
	LocalDateTime createdAt, LocalDateTime calledAt, LocalDateTime completedAt) {}
