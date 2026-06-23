package com.hospital.queue.dto;
import com.hospital.queue.model.QueueStatus;
public record CurrentQueueResponse(String departmentCode, String departmentName,
	String currentQueueNumber, String counterName, QueueStatus status,
	long waitingCount, long usedSlots, int dailyQuota) {}
