package com.hospital.queue.service;

import com.hospital.queue.constant.QueueRules;
import com.hospital.queue.constant.ResponseMessages;
import com.hospital.queue.domain.Department;
import com.hospital.queue.domain.PriorityCategory;
import com.hospital.queue.domain.QueueStatus;
import com.hospital.queue.domain.QueueTicket;
import com.hospital.queue.dto.CallNextRequest;
import com.hospital.queue.dto.CurrentQueueResponse;
import com.hospital.queue.dto.DepartmentResponse;
import com.hospital.queue.dto.QueueTicketResponse;
import com.hospital.queue.dto.TakeQueueRequest;
import com.hospital.queue.dto.UpdateTicketStatusRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class QueueService {
	private static final Comparator<QueueTicket> QUEUE_ORDER = Comparator
		.comparingInt((QueueTicket ticket) -> ticket.getPriorityCategory() == PriorityCategory.PRIORITY ? 0 : 1)
		.thenComparing(QueueTicket::getId);

	private final Clock clock;
	private final AtomicLong idSequence = new AtomicLong(QueueRules.INITIAL_TICKET_ID);
	private final List<QueueTicket> tickets = new ArrayList<>();
	private final Map<Department, Integer> departmentSequences = new EnumMap<>(Department.class);
	private LocalDate activeDate;

	public QueueService(Clock clock) {
		this.clock = clock;
		this.activeDate = LocalDate.now(clock);
		for (Department department : Department.values()) {
			departmentSequences.put(department, QueueRules.INITIAL_SEQUENCE);
		}
	}

	public synchronized QueueTicketResponse takeQueue(TakeQueueRequest request) {
		resetIfNewDay();
		LocalDateTime now = LocalDateTime.now(clock);
		if (now.toLocalTime().isBefore(QueueRules.QUEUE_OPEN_TIME)) {
			throw new BusinessRuleException(HttpStatus.CONFLICT, ResponseMessages.QUEUE_NOT_OPEN);
		}

		Department department = parseDepartment(request.departmentCode());
		String normalizedIc = normalizeIc(request.icNumber());
		validateDailyQuota(department);
		validateNoActiveDuplicate(normalizedIc, department);

		int nextSequence = departmentSequences.compute(
			department,
			(key, value) -> value == null ? QueueRules.FIRST_SEQUENCE : value + QueueRules.FIRST_SEQUENCE
		);
		String queueNumber = department.name() + QueueRules.QUEUE_NUMBER_SEQUENCE_FORMAT.formatted(nextSequence);
		QueueTicket ticket = new QueueTicket(
			idSequence.getAndIncrement(),
			queueNumber,
			request.patientName().trim(),
			normalizedIc,
			request.phoneNumber().trim(),
			department,
			request.visitReason().trim(),
			request.priorityCategory(),
			activeDate,
			now
		);
		tickets.add(ticket);

		return toResponse(ticket);
	}

	public synchronized QueueTicketResponse getTicket(String queueNumber) {
		resetIfNewDay();
		return toResponse(findTicket(queueNumber));
	}

	public synchronized QueueTicketResponse updateStatus(String queueNumber, UpdateTicketStatusRequest request) {
		resetIfNewDay();
		QueueTicket ticket = findTicket(queueNumber);
		LocalDateTime now = LocalDateTime.now(clock);

		ticket.setStatus(request.status());
		if (request.status() == QueueStatus.COMPLETED) {
			ticket.setCompletedAt(now);
		}

		return toResponse(ticket);
	}

	public synchronized QueueTicketResponse callNext(CallNextRequest request) {
		resetIfNewDay();
		LocalDateTime now = LocalDateTime.now(clock);
		validateNotLunchBreak(now);

		Department department = parseDepartment(request.departmentCode());
		QueueTicket ticket = tickets.stream()
			.filter(candidate -> candidate.getDepartment() == department)
			.filter(candidate -> candidate.getStatus() == QueueStatus.WAITING)
			.min(QUEUE_ORDER)
			.orElseThrow(() -> new BusinessRuleException(
				HttpStatus.NOT_FOUND,
				ResponseMessages.NO_WAITING_TICKET.formatted(department.getDisplayName())
			));

		ticket.setStatus(QueueStatus.CALLED);
		ticket.setCounterName(request.counterName().trim());
		ticket.setCalledAt(now);
		return toResponse(ticket);
	}

	public synchronized List<DepartmentResponse> getDepartments() {
		return List.of(Department.values()).stream()
			.map(department -> new DepartmentResponse(
				department.name(),
				department.getDisplayName(),
				department.getDailyQuota()
			))
			.toList();
	}

	public synchronized List<CurrentQueueResponse> getCurrentQueues() {
		resetIfNewDay();
		return List.of(Department.values()).stream()
			.map(department -> new CurrentQueueResponse(
				department.name(),
				department.getDisplayName(),
				currentQueueNumber(department),
				waitingCount(department),
				usedSlots(department),
				department.getDailyQuota()
			))
			.toList();
	}

	private QueueTicket findTicket(String queueNumber) {
		return tickets.stream()
			.filter(ticket -> ticket.getQueueNumber().equalsIgnoreCase(queueNumber))
			.findFirst()
			.orElseThrow(() -> new BusinessRuleException(HttpStatus.NOT_FOUND, ResponseMessages.QUEUE_NUMBER_NOT_FOUND));
	}

	private Department parseDepartment(String departmentCode) {
		try {
			return Department.fromCode(departmentCode.trim());
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessRuleException(HttpStatus.BAD_REQUEST, ex.getMessage());
		}
	}

	private String normalizeIc(String icNumber) {
		String normalized = icNumber.replaceAll(QueueRules.NON_DIGIT_REGEX, "");
		if (normalized.length() != QueueRules.IC_DIGIT_COUNT) {
			throw new BusinessRuleException(HttpStatus.BAD_REQUEST, ResponseMessages.INVALID_IC_NUMBER);
		}
		return normalized;
	}

	private void validateNotLunchBreak(LocalDateTime now) {
		if (!now.toLocalTime().isBefore(QueueRules.LUNCH_BREAK_START_TIME)
				&& now.toLocalTime().isBefore(QueueRules.LUNCH_BREAK_END_TIME)) {
			throw new BusinessRuleException(HttpStatus.CONFLICT, ResponseMessages.LUNCH_BREAK_CALLING_PAUSED);
		}
	}

	private void validateDailyQuota(Department department) {
		int usedSlots = usedSlots(department);
		if (usedSlots >= department.getDailyQuota()) {
			throw new BusinessRuleException(
				HttpStatus.CONFLICT,
				ResponseMessages.DAILY_QUOTA_FULL.formatted(department.getDisplayName())
			);
		}
	}

	private void validateNoActiveDuplicate(String icNumber, Department department) {
		tickets.stream()
			.filter(ticket -> ticket.getDepartment() == department)
			.filter(ticket -> ticket.getIcNumber().equals(icNumber))
			.filter(ticket -> QueueRules.ACTIVE_STATUSES.contains(ticket.getStatus()))
			.findFirst()
			.ifPresent(ticket -> {
				throw new BusinessRuleException(
					HttpStatus.CONFLICT,
					ResponseMessages.DUPLICATE_ACTIVE_IC.formatted(
						department.getDisplayName(),
						ticket.getQueueNumber()
					)
				);
			});
	}

	private QueueTicketResponse toResponse(QueueTicket ticket) {
		return new QueueTicketResponse(
			ticket.getQueueNumber(),
			ticket.getPatientName(),
			ticket.getIcNumber(),
			ticket.getPhoneNumber(),
			ticket.getDepartment().name(),
			ticket.getDepartment().getDisplayName(),
			ticket.getVisitReason(),
			ticket.getPriorityCategory(),
			ticket.getStatus(),
			ticket.getCounterName(),
			peopleAhead(ticket),
			ticket.getQueueDate(),
			ticket.getCreatedAt(),
			ticket.getCalledAt(),
			ticket.getCompletedAt()
		);
	}

	private int peopleAhead(QueueTicket ticket) {
		return (int) tickets.stream()
			.filter(candidate -> candidate.getDepartment() == ticket.getDepartment())
			.filter(candidate -> candidate.getStatus() == QueueStatus.WAITING)
			.filter(candidate -> QUEUE_ORDER.compare(candidate, ticket) < 0)
			.count();
	}

	private String currentQueueNumber(Department department) {
		return tickets.stream()
			.filter(ticket -> ticket.getDepartment() == department)
			.filter(ticket -> ticket.getStatus() == QueueStatus.SERVING || ticket.getStatus() == QueueStatus.CALLED)
			.max(Comparator.comparing(QueueTicket::getId))
			.map(QueueTicket::getQueueNumber)
			.orElse(null);
	}

	private int waitingCount(Department department) {
		return (int) tickets.stream()
			.filter(ticket -> ticket.getDepartment() == department)
			.filter(ticket -> ticket.getStatus() == QueueStatus.WAITING)
			.count();
	}

	private int usedSlots(Department department) {
		return (int) tickets.stream()
			.filter(ticket -> ticket.getDepartment() == department)
			.count();
	}

	private void resetIfNewDay() {
		LocalDate today = LocalDate.now(clock);
		if (!today.equals(activeDate)) {
			tickets.clear();
			idSequence.set(QueueRules.INITIAL_TICKET_ID);
			departmentSequences.replaceAll((department, sequence) -> QueueRules.INITIAL_SEQUENCE);
			activeDate = today;
		}
	}
}
