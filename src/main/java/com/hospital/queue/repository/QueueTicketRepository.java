package com.hospital.queue.repository;

import com.hospital.queue.domain.QueueTicket;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueTicketRepository extends JpaRepository<QueueTicket, Long> {
	List<QueueTicket> findByQueueDateOrderByIdAsc(LocalDate queueDate);
}
