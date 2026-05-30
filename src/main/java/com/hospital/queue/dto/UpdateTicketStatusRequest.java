package com.hospital.queue.dto;

import com.hospital.queue.domain.QueueStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(
		@NotNull(message = "Ticket status is required")
		QueueStatus status) {
}
