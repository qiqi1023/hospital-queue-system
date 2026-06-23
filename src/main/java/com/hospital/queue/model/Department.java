package com.hospital.queue.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "departments")
public class Department {
	@Id
	@Column(length = 10)
	private String code;
	@Column(nullable = false)
	private String name;
	@Column(nullable = false, length = 10)
	private String prefix;
	@Column(nullable = false)
	private int dailyQuota;
	@Column(nullable = false)
	private LocalTime openingTime;
	@Column(nullable = false)
	private LocalTime closingTime;
	private LocalTime breakStartTime;
	private LocalTime breakEndTime;

	protected Department() {}
	public String getCode() { return code; }
	public String getName() { return name; }
	public String getPrefix() { return prefix; }
	public int getDailyQuota() { return dailyQuota; }
	public LocalTime getOpeningTime() { return openingTime; }
	public LocalTime getClosingTime() { return closingTime; }
	public LocalTime getBreakStartTime() { return breakStartTime; }
	public LocalTime getBreakEndTime() { return breakEndTime; }
}
