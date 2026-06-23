package com.hospital.queue.dto;
import com.hospital.queue.model.QueueStatus;
import jakarta.validation.constraints.NotNull;
public record StatusRequest(@NotNull(message = "Please select a ticket status") QueueStatus status) {}
