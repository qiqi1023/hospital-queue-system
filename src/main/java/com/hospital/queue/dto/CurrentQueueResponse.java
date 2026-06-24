package com.hospital.queue.dto;
import java.time.LocalDateTime;
import com.hospital.queue.model.QueueStatus;
public record CurrentQueueResponse(String departmentCode, String departmentName,
	String currentQueueNumber, String counterName, QueueStatus status, LocalDateTime calledAt,
	long waitingCount, long usedSlots, int dailyQuota) {}
