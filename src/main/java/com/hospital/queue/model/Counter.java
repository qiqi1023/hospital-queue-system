package com.hospital.queue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "counters")
public class Counter {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false, unique = true)
	private String name;
	@Column(nullable = false)
	private String departmentCode;
	@Enumerated(EnumType.STRING) @Column(nullable = false)
	private CounterStatus status;
	protected Counter() {}
	public Long getId() { return id; }
	public String getName() { return name; }
	public String getDepartmentCode() { return departmentCode; }
	public CounterStatus getStatus() { return status; }
}
