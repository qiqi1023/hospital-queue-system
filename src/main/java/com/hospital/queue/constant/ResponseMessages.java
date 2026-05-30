package com.hospital.queue.constant;

public final class ResponseMessages {
	public static final String UNKNOWN_DEPARTMENT_CODE = "Unknown department code: %s";
	public static final String QUEUE_NOT_OPEN = "Online queue opens daily at 6:00 AM.";
	public static final String QUEUE_CLOSED = "Online queue closes daily at 10:00 PM.";
	public static final String QUEUE_NUMBER_NOT_FOUND = "Queue number not found.";
	public static final String INVALID_IC_NUMBER = "IC number must contain exactly 12 digits.";
	public static final String DAILY_QUOTA_FULL = "Daily quota for %s is full.";
	public static final String DUPLICATE_ACTIVE_IC = "IC number already has an active ticket for %s: %s";
	public static final String REQUEST_VALIDATION_FAILED = "Request validation failed.";
	public static final String LUNCH_BREAK_CALLING_PAUSED = "Queue calling is paused during lunch break from 12:00 PM to 2:00 PM.";
	public static final String NO_WAITING_TICKET = "No waiting ticket found for %s.";
	public static final String STATUS_UPDATED = "Ticket status updated successfully.";
	public static final String INVALID_STATUS_TRANSITION = "Ticket status cannot be changed from %s to %s.";
	public static final String UNKNOWN_STATUS = "Unknown ticket status: %s";

	private ResponseMessages() {
	}
}
