package com.hospital.queue.repository;

import com.hospital.queue.model.IdentityType;
import com.hospital.queue.model.QueueStatus;
import com.hospital.queue.model.QueueTicket;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface QueueTicketRepo extends JpaRepository<QueueTicket, Long>, JpaSpecificationExecutor<QueueTicket> {
	Optional<QueueTicket> findByQueueNumberIgnoreCaseAndQueueDate(String queueNumber, LocalDate queueDate);
	boolean existsByIdentityTypeAndIdentityNumberAndDepartmentCodeAndQueueDateAndStatusIn(
		IdentityType type, String number, String departmentCode, LocalDate date, Collection<QueueStatus> statuses);
	long countByDepartmentCodeAndQueueDate(String departmentCode, LocalDate date);
	@Query("""
		select q from QueueTicket q
		where q.departmentCode = :departmentCode and q.queueDate = :queueDate and q.status = :status
		order by case when q.calledAt is null then 0 else 1 end, q.calledAt, q.id
		""")
	List<QueueTicket> findWaitingInCallOrder(String departmentCode, LocalDate queueDate, QueueStatus status);
	boolean existsByCounterNameIgnoreCaseAndQueueDateAndStatusIn(
		String counterName, LocalDate date, Collection<QueueStatus> statuses);
	long countByDepartmentCodeAndQueueDateAndStatus(String departmentCode, LocalDate date, QueueStatus status);
	long countByDepartmentCodeAndQueueDateAndStatusAndIdLessThan(String departmentCode, LocalDate date, QueueStatus status, Long id);
	Optional<QueueTicket> findFirstByDepartmentCodeAndQueueDateAndStatusInOrderByCalledAtDesc(
		String departmentCode, LocalDate date, Collection<QueueStatus> statuses);
	List<QueueTicket> findByQueueDateAndStatusInOrderByCounterNameAsc(
		LocalDate date, Collection<QueueStatus> statuses);

	Optional<QueueTicket> findFirstByDepartmentCodeAndQueueDateOrderByIdDesc(String code, LocalDate date);
}
