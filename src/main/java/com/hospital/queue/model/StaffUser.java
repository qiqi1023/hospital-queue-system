package com.hospital.queue.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_users")
public class StaffUser {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(nullable = false, length = 30)
	private String role;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	protected StaffUser() {}

	public String getUsername() { return username; }
	public String getPasswordHash() { return passwordHash; }
	public String getRole() { return role; }
	public boolean isEnabled() { return enabled; }
}
