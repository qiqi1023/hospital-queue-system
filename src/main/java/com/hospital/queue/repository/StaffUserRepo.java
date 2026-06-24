package com.hospital.queue.repository;

import com.hospital.queue.model.StaffUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffUserRepo extends JpaRepository<StaffUser, Long> {
	Optional<StaffUser> findByUsernameIgnoreCaseAndEnabledTrue(String username);
}
