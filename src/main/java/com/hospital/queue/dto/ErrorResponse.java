package com.hospital.queue.dto;
import java.util.Map;
public record ErrorResponse(boolean success, String message, Map<String, String> errors) {
	public static ErrorResponse of(String message) { return new ErrorResponse(false, message, Map.of()); }
}
