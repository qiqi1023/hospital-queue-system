package com.hospital.queue.model;

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
@Table(name = "queue_tickets", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = {"queue_number", "queue_date"}))
public class QueueTicket {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String queueNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IdentityType identityType;

	@Column(nullable = false, length = 20)
	private String identityNumber;

	@Column(nullable = false, length = 10)
	private String phoneCountryCode;

	@Column(nullable = false)
	private String phoneNumber;

	@Column(nullable = false)
	private String departmentCode;

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

	public QueueTicket(String queueNumber, IdentityType identityType, String identityNumber,
			String phoneCountryCode, String phoneNumber, String departmentCode,
			LocalDate queueDate, LocalDateTime createdAt) {
		this.queueNumber = queueNumber;
		this.identityType = identityType;
		this.identityNumber = identityNumber;
		this.phoneCountryCode = phoneCountryCode;
		this.phoneNumber = phoneNumber;
		this.departmentCode = departmentCode;
		this.status = QueueStatus.WAITING;
		this.queueDate = queueDate;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getQueueNumber() {
		return queueNumber;
	}

	public IdentityType getIdentityType() { return identityType; }
	public String getIdentityNumber() { return identityNumber; }
	public String getPhoneCountryCode() { return phoneCountryCode; }

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getDepartmentCode() { return departmentCode; }

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
