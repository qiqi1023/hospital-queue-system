package com.hospital.queue.service;

import com.hospital.queue.dto.*;
import com.hospital.queue.exception.*;
import com.hospital.queue.model.*;
import com.hospital.queue.repository.*;
import jakarta.persistence.criteria.Predicate;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QueueService {
	private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
	private static final List<QueueStatus> ACTIVE = List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.SERVING);
	private static final Map<QueueStatus, Set<QueueStatus>> TRANSITIONS = Map.of(
		QueueStatus.WAITING, Set.of(QueueStatus.CANCELLED),
		QueueStatus.CALLED, Set.of(QueueStatus.SERVING, QueueStatus.COMPLETED, QueueStatus.MISSED, QueueStatus.CANCELLED),
		QueueStatus.SERVING, Set.of(QueueStatus.COMPLETED),
		QueueStatus.COMPLETED, Set.of(), QueueStatus.MISSED, Set.of(QueueStatus.WAITING), QueueStatus.CANCELLED, Set.of());
	private final QueueTicketRepo tickets;
	private final DepartmentRepo departments;
	private final CounterRepo counters;
	private final IcStateRepo states;
	private final PhoneCodeRepo phoneCodes;
	private final Clock clock;

	public QueueService(QueueTicketRepo tickets, DepartmentRepo departments, CounterRepo counters,
			IcStateRepo states, PhoneCodeRepo phoneCodes, Clock clock) {
		this.tickets = tickets; this.departments = departments; this.counters = counters;
		this.states = states; this.phoneCodes = phoneCodes; this.clock = clock;
	}

	@Transactional
	public QueueResponse takeQueue(TakeQueueRequest request) {
		String code = normalizeCode(request.departmentCode());
		Department department = departments.lockByCode(code).orElseThrow(() -> new NotFoundException("Department not found"));
		LocalDateTime now = LocalDateTime.now(clock);
		if (now.toLocalTime().isBefore(department.getOpeningTime()) || !now.toLocalTime().isBefore(department.getClosingTime()))
			throw new ConflictException("Online registration is currently closed. It is available from "
				+ displayTime(department.getOpeningTime()) + " to " + displayTime(department.getClosingTime()) + ".");
		String identity = validateIdentity(request.identityType(), request.identityNumber(), now.toLocalDate());
		String dialCode = request.phoneCountryCode().trim();
		if (!phoneCodes.existsByDialCode(dialCode)) throw new BadRequestException("Please select a valid country code.");
		String phone = validatePhone(dialCode, request.phoneNumber());
		if (tickets.existsByIdentityTypeAndIdentityNumberAndDepartmentCodeAndQueueDateAndStatusIn(
			request.identityType(), identity, code, now.toLocalDate(), ACTIVE))
			throw new ConflictException("You already have an active queue ticket for this department today.");
		long used = tickets.countByDepartmentCodeAndQueueDate(code, now.toLocalDate());
		if (used >= department.getDailyQuota())
			throw new ConflictException("All queue numbers for this department have been taken today. Please try again tomorrow.");
		String queueNumber = department.getPrefix() + "%03d".formatted(nextSequence(department, now.toLocalDate()));
		QueueTicket ticket = tickets.save(new QueueTicket(queueNumber, request.identityType(), identity,
			dialCode, phone, code, now.toLocalDate(), now));
		return response(ticket, department);
	}

	public QueueResponse getTicket(String queueNumber) {
		QueueTicket ticket = findToday(queueNumber);
		return response(ticket, department(ticket.getDepartmentCode()));
	}

	public Page<QueueResponse> list(String departmentCode, QueueStatus status, LocalDate queueDate, Pageable pageable) {
		String code = departmentCode == null || departmentCode.isBlank() ? null : normalizeCode(departmentCode);
		if (code != null) department(code);
		Specification<QueueTicket> spec = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (code != null) predicates.add(cb.equal(root.get("departmentCode"), code));
			if (status != null) predicates.add(cb.equal(root.get("status"), status));
			if (queueDate != null) predicates.add(cb.equal(root.get("queueDate"), queueDate));
			return cb.and(predicates.toArray(Predicate[]::new));
		};
		Map<String, Department> refs = new HashMap<>();
		return tickets.findAll(spec, pageable).map(ticket -> response(ticket,
			refs.computeIfAbsent(ticket.getDepartmentCode(), this::department)));
	}

	@Transactional
	public QueueResponse callNext(CallRequest request) {
		String code = normalizeCode(request.departmentCode());
		Department department = departments.lockByCode(code).orElseThrow(() -> new NotFoundException("Department not found"));
		LocalDateTime now = LocalDateTime.now(clock);
		if (inBreak(department, now.toLocalTime()))
			throw new ConflictException("The counter is currently on break. Service will resume at "
				+ displayTime(department.getBreakEndTime()) + ".");
		Counter counter = counters.findByNameIgnoreCase(request.counterName().trim())
			.orElseThrow(() -> new NotFoundException("Counter not found"));
		if (!counter.getDepartmentCode().equalsIgnoreCase(code)) throw new BadRequestException("Invalid counter");
		if (counter.getStatus() != CounterStatus.OPEN) throw new ConflictException("This counter is currently unavailable. Please select another counter.");
		if (tickets.existsByCounterNameIgnoreCaseAndQueueDateAndStatusIn(counter.getName(), now.toLocalDate(), ACTIVE))
			throw new ConflictException("This counter is currently serving another patient. Please select another counter.");
		QueueTicket ticket = tickets.findWaitingInCallOrder(code, now.toLocalDate(), QueueStatus.WAITING).stream().findFirst()
			.orElseThrow(() -> new NotFoundException("Ticket not found"));
		ticket.setStatus(QueueStatus.CALLED); ticket.setCounterName(counter.getName()); ticket.setCalledAt(now);
		return response(tickets.save(ticket), department);
	}

	@Transactional
	public QueueResponse callTicket(String queueNumber, CallTicketRequest request) {
		QueueTicket ticket = tickets.findByQueueNumberIgnoreCaseAndQueueDate(queueNumber.trim(), LocalDate.now(clock))
			.orElseThrow(() -> new NotFoundException("Ticket not found"));
		if (ticket.getStatus() != QueueStatus.WAITING)
			throw new ConflictException("Only waiting tickets can be called.");
		Counter counter = counters.findByNameIgnoreCase(request.counterName().trim())
			.orElseThrow(() -> new NotFoundException("Counter not found"));
		if (!counter.getDepartmentCode().equalsIgnoreCase(ticket.getDepartmentCode()))
			throw new BadRequestException("Invalid counter");
		if (counter.getStatus() != CounterStatus.OPEN)
			throw new ConflictException("This counter is currently unavailable. Please select another counter.");
		if (tickets.existsByCounterNameIgnoreCaseAndQueueDateAndStatusIn(counter.getName(), LocalDate.now(clock), ACTIVE))
			throw new ConflictException("This counter is currently serving another patient. Please select another counter.");
		ticket.setStatus(QueueStatus.CALLED);
		ticket.setCounterName(counter.getName());
		ticket.setCalledAt(LocalDateTime.now(clock));
		return response(tickets.save(ticket), department(ticket.getDepartmentCode()));
	}

	@Transactional
	public QueueResponse updateStatus(String queueNumber, StatusRequest request) {
		QueueTicket ticket = findToday(queueNumber);
		QueueStatus previousStatus = ticket.getStatus();
		if (!TRANSITIONS.getOrDefault(previousStatus, Set.of()).contains(request.status()))
			throw new ConflictException("This ticket cannot be changed to the selected status.");
		ticket.setStatus(request.status());
		if (previousStatus == QueueStatus.MISSED && request.status() == QueueStatus.WAITING) {
			ticket.setCounterName(null);
			ticket.setCalledAt(LocalDateTime.now(clock));
		}
		if (request.status() == QueueStatus.COMPLETED) ticket.setCompletedAt(LocalDateTime.now(clock));
		return response(tickets.save(ticket), department(ticket.getDepartmentCode()));
	}

	public List<CurrentQueueResponse> currentQueues(String requestedCode) {
		LocalDate date = LocalDate.now(clock);
		List<Department> list = requestedCode == null || requestedCode.isBlank()
			? departments.findAll(Sort.by("code")) : List.of(department(normalizeCode(requestedCode)));
		return list.stream().map(d -> {
			QueueTicket current = tickets.findFirstByDepartmentCodeAndQueueDateAndStatusInOrderByCalledAtDesc(
				d.getCode(), date, List.of(QueueStatus.CALLED, QueueStatus.SERVING)).orElse(null);
			return new CurrentQueueResponse(d.getCode(), d.getName(), current == null ? null : current.getQueueNumber(),
				current == null ? null : current.getCounterName(), current == null ? null : current.getStatus(),
				current == null ? null : current.getCalledAt(),
				tickets.countByDepartmentCodeAndQueueDateAndStatus(d.getCode(), date, QueueStatus.WAITING),
				tickets.countByDepartmentCodeAndQueueDate(d.getCode(), date), d.getDailyQuota());
		}).toList();
	}

	private String validateIdentity(IdentityType type, String value, LocalDate today) {
		String identity = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
		if (type == IdentityType.NON_MALAYSIAN) {
			if (!identity.matches("[A-Z0-9]{5,20}"))
				throw new BadRequestException("Please enter a valid passport or foreign identity number.");
			return identity;
		}
		if (identity.length() != 12) throw invalidIdentity();
		if (!identity.matches("\\d{12}")) throw invalidIdentity();
		try {
			int yy = Integer.parseInt(identity.substring(0, 2));
			int year = yy <= today.getYear() % 100 ? 2000 + yy : 1900 + yy;
			LocalDate birthDate = LocalDate.of(year, Integer.parseInt(identity.substring(2, 4)), Integer.parseInt(identity.substring(4, 6)));
			if (birthDate.isAfter(today)) throw invalidIdentity();
		} catch (DateTimeException | NumberFormatException ex) { throw invalidIdentity(); }
		if (!states.existsById(identity.substring(6, 8))) throw invalidIdentity();
		return identity;
	}

	private String validatePhone(String dialCode, String value) {
		String phone = value == null ? "" : value.trim();
		if (!phone.matches("\\d{4,15}")) throw invalidPhone();
		if ("+60".equals(dialCode)) {
			if (!phone.startsWith("0")) phone = "0" + phone;
			if (!phone.matches("01\\d{8,9}")) throw invalidPhone();
		}
		return phone;
	}

	private int nextSequence(Department department, LocalDate date) {
		return tickets.findFirstByDepartmentCodeAndQueueDateOrderByIdDesc(department.getCode(), date)
			.map(t -> Integer.parseInt(t.getQueueNumber().substring(department.getPrefix().length())) + 1).orElse(1);
	}
	private BadRequestException invalidIdentity() { return new BadRequestException("Please enter a valid identity or passport number."); }
	private BadRequestException invalidPhone() { return new BadRequestException("Please enter a valid mobile number."); }
	private String displayTime(LocalTime time) { return time.format(DISPLAY_TIME); }
	private boolean inBreak(Department d, LocalTime time) { return d.getBreakStartTime() != null && d.getBreakEndTime() != null
		&& !time.isBefore(d.getBreakStartTime()) && time.isBefore(d.getBreakEndTime()); }
	private QueueTicket findToday(String number) { return tickets.findByQueueNumberIgnoreCaseAndQueueDate(number.trim(), LocalDate.now(clock))
		.orElseThrow(() -> new NotFoundException("Ticket not found")); }
	private Department department(String code) { return departments.findById(code)
		.orElseThrow(() -> new NotFoundException("Department not found")); }
	private String normalizeCode(String code) { return code.trim().toUpperCase(Locale.ROOT); }
	private QueueResponse response(QueueTicket ticket, Department department) {
		long ahead = 0;
		if (ticket.getStatus() == QueueStatus.WAITING) {
			List<QueueTicket> waiting = tickets.findWaitingInCallOrder(
				ticket.getDepartmentCode(), ticket.getQueueDate(), QueueStatus.WAITING);
			for (int index = 0; index < waiting.size(); index++) {
				if (Objects.equals(waiting.get(index).getId(), ticket.getId())) {
					ahead = index;
					break;
				}
			}
		}
		return new QueueResponse(ticket.getQueueNumber(), ticket.getDepartmentCode(), department.getName(), ticket.getStatus(),
			ticket.getCounterName(), ahead, ticket.getQueueDate(), ticket.getCreatedAt(), ticket.getCalledAt(), ticket.getCompletedAt());
	}
}
