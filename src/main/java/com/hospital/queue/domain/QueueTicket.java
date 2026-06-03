package com.hospital.queue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "queue_tickets")
public class QueueTicket {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long databaseId;

	@Column(nullable = false)
	private long id;

	@Column(nullable = false)
	private String queueNumber;

	@Column(nullable = false)
	private String patientName;

	@Column(nullable = false)
	private String icNumber;

	@Column(nullable = false)
	private String phoneNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Department department;

	@Column(nullable = false)
	private String visitReason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PriorityCategory priorityCategory;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private QueueStatus status;

	private String counterName;

	@Column(nullable = false)
	private LocalDate queueDate;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	private LocalDateTime calledAt;
	private LocalDateTime completedAt;

	protected QueueTicket() {
	}

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
