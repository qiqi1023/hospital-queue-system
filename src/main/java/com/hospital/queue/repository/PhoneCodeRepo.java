package com.hospital.queue.repository;
import com.hospital.queue.model.PhoneCode;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PhoneCodeRepo extends JpaRepository<PhoneCode, Long> {
	boolean existsByDialCode(String dialCode);
}
