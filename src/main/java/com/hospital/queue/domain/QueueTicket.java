package com.hospital.queue.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class QueueTicket {
	private final long id;
	private final String queueNumber;
	private final String patientName;
	private final String icNumber;
	private final String phoneNumber;
	private final Department department;
	private final String visitReason;
	private final PriorityCategory priorityCategory;
	private QueueStatus status;
	private String counterName;
	private final LocalDate queueDate;
	private final LocalDateTime createdAt;
	private LocalDateTime calledAt;
	private LocalDateTime completedAt;

	public QueueTicket(
			long id,
			String queueNumber,
			String patientName,
			String icNumber,
			String phoneNumber,
			Department department,
			String visitReason,
			PriorityCategory priorityCategory,
			LocalDate queueDate,
			LocalDateTime createdAt) {
		this.id = id;
		this.queueNumber = queueNumber;
		this.patientName = patientName;
		this.icNumber = icNumber;
		this.phoneNumber = phoneNumber;
		this.department = department;
		this.visitReason = visitReason;
		this.priorityCategory = priorityCategory;
		this.status = QueueStatus.WAITING;
		this.queueDate = queueDate;
		this.createdAt = createdAt;
	}

	public long getId() {
		return id;
	}

	public String getQueueNumber() {
		return queueNumber;
	}

	public String getPatientName() {
		return patientName;
	}

	public String getIcNumber() {
		return icNumber;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public Department getDepartment() {
		return department;
	}

	public String getVisitReason() {
		return visitReason;
	}

	public PriorityCategory getPriorityCategory() {
		return priorityCategory;
	}

	public QueueStatus getStatus() {
		return status;
	}

	public void setStatus(QueueStatus status) {
		this.status = status;
	}

	public String getCounterName() {
		return counterName;
	}

	public void setCounterName(String counterName) {
		this.counterName = counterName;
	}

	public LocalDate getQueueDate() {
		return queueDate;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getCalledAt() {
		return calledAt;
	}

	public void setCalledAt(LocalDateTime calledAt) {
		this.calledAt = calledAt;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}
}
