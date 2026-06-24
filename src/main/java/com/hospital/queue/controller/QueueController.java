package com.hospital.queue.controller;

import com.hospital.queue.dto.*;
import com.hospital.queue.exception.BadRequestException;
import com.hospital.queue.model.QueueStatus;
import com.hospital.queue.service.QueueService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/api/queueTickets", produces=MediaType.APPLICATION_JSON_VALUE)
public class QueueController {
	private static final Set<String> SORT_FIELDS = Set.of("id", "queueNumber", "departmentCode", "status", "queueDate", "createdAt", "calledAt", "completedAt");
	private final QueueService service;
	public QueueController(QueueService service) { this.service = service; }

	@PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<QueueResponse> create(@Valid @RequestBody TakeQueueRequest request) {
		return ApiResponse.ok("Queue number successfully generated", service.takeQueue(request));
	}
	@GetMapping("/{queueNumber}")
	public ApiResponse<QueueResponse> get(@PathVariable String queueNumber) {
		return ApiResponse.ok("Ticket found", service.getTicket(queueNumber));
	}
	@GetMapping
	public ApiResponse<Page<QueueResponse>> list(@RequestParam(required=false) String departmentCode,
			@RequestParam(required=false) String status,
			@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate queueDate,
			@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
			@RequestParam(defaultValue="createdAt,asc") String sort) {
		if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Invalid pagination");
		String[] parts = sort.split(",", 2); String field = parts[0];
		if (!SORT_FIELDS.contains(field)) throw new BadRequestException("Invalid sort");
		Sort.Direction direction = parts.length == 2 && parts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		QueueStatus parsed = null;
		try { if (status != null && !status.isBlank()) parsed = QueueStatus.valueOf(status.toUpperCase()); }
		catch (IllegalArgumentException ex) { throw new BadRequestException("Please select a valid ticket status."); }
		// Default to today's date if not provided
		if (queueDate == null) queueDate = LocalDate.now();
		return ApiResponse.ok("Tickets successfully retrieved", service.list(departmentCode, parsed, queueDate, PageRequest.of(page, size, Sort.by(direction, field))));
	}
	@PostMapping(value="/{queueNumber}/call", consumes=MediaType.APPLICATION_JSON_VALUE)
	public ApiResponse<QueueResponse> call(@PathVariable String queueNumber, @Valid @RequestBody CallTicketRequest request) {
		return ApiResponse.ok("Ticket called", service.callTicket(queueNumber, request));
	}

	@PatchMapping(value="/{queueNumber}/status", consumes=MediaType.APPLICATION_JSON_VALUE)
	public ApiResponse<QueueResponse> status(@PathVariable String queueNumber, @Valid @RequestBody StatusRequest request) {
		return ApiResponse.ok("Status updated", service.updateStatus(queueNumber, request));
	}
}
