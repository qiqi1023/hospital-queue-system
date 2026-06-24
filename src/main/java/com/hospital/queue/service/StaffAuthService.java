package com.hospital.queue.service;

import com.hospital.queue.repository.StaffUserRepo;
import com.hospital.queue.model.StaffUser;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class StaffAuthService {
	public static final String SESSION_ATTRIBUTE = "staffAuthenticated";
	public static final String SESSION_USERNAME = "staffUsername";

	private final StaffUserRepo staffUsers;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public StaffAuthService(StaffUserRepo staffUsers) {
		this.staffUsers = staffUsers;
	}

	public boolean authenticate(String suppliedUsername, String suppliedPassword) {
		return authenticateUser(suppliedUsername, suppliedPassword).isPresent();
	}

	public Optional<StaffUser> authenticateUser(String suppliedUsername, String suppliedPassword) {
		if (suppliedUsername == null || suppliedPassword == null) return Optional.empty();
		return staffUsers.findByUsernameIgnoreCaseAndEnabledTrue(suppliedUsername.trim())
				.filter(user -> passwordEncoder.matches(suppliedPassword, user.getPasswordHash()));
	}
}
