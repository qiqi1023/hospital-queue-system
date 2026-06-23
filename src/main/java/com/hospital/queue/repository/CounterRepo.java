package com.hospital.queue.repository;
import com.hospital.queue.model.Counter;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CounterRepo extends JpaRepository<Counter, Long> {
	Optional<Counter> findByNameIgnoreCase(String name);
	List<Counter> findByDepartmentCodeIgnoreCaseOrderByName(String code);
}
