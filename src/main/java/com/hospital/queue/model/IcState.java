package com.hospital.queue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ic_states")
public class IcState {
	@Id @Column(length = 2)
	private String code;
	@Column(nullable = false)
	private String stateName;
	protected IcState() {}
	public String getCode() { return code; }
	public String getStateName() { return stateName; }
}
