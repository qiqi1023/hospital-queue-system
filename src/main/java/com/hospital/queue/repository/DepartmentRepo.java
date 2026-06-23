package com.hospital.queue.repository;
import com.hospital.queue.model.Department;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface DepartmentRepo extends JpaRepository<Department, String> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from Department d where d.code=:code")
	Optional<Department> lockByCode(@Param("code") String code);
}
