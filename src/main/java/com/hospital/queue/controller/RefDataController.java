package com.hospital.queue.controller;

import com.hospital.queue.dto.ApiResponse;
import com.hospital.queue.service.QueueService;
import com.hospital.queue.service.RefDataService;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class RefDataController {
	private final RefDataService refs;
	private final QueueService queues;

	public RefDataController(RefDataService refs, QueueService queues) {
		this.refs = refs;
		this.queues = queues;
	}

	@GetMapping("/departments")
	public ApiResponse<?> departments() {
		return ApiResponse.ok("Departments successfully retrieved", refs.departments());
	}

	@GetMapping("/departments/{code}")
	public ApiResponse<?> department(@PathVariable String code) {
		return ApiResponse.ok("Department successfully found", refs.department(code));
	}

	@GetMapping("/departments/{code}/queueTickets")
	public ApiResponse<?> tickets(@PathVariable String code,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		int safeSize = Math.min(Math.max(size, 1), 100);
		return ApiResponse.ok("Tickets successfully retrieved",
			queues.list(code, null, LocalDate.now(),
				PageRequest.of(page, safeSize, Sort.by("createdAt"))));
	}

	@GetMapping("/departments/{code}/counters")
	public ApiResponse<?> departmentCounters(@PathVariable String code) {
		return ApiResponse.ok("Counters successfully retrieved", refs.counters(code));
	}

	@GetMapping("/counters")
	public ApiResponse<?> counters() {
		return ApiResponse.ok("Counters successfully retrieved", refs.counters());
	}

	@GetMapping("/phoneCodes")
	public ApiResponse<?> phoneCodes() {
		return ApiResponse.ok("Phone codes successfully retrieved", refs.phoneCodes());
	}

	@GetMapping("/icStates")
	public ApiResponse<?> icStates() {
		return ApiResponse.ok("IC states successfully retrieved", refs.icStates());
	}

	@GetMapping("/queues/current")
	public ApiResponse<?> current(@RequestParam(required = false) String departmentCode) {
		return ApiResponse.ok("Current queues successfully retrieved", queues.currentQueues(departmentCode));
	}

	@GetMapping("/queues/activeServices")
	public ApiResponse<?> activeServices() {
		return ApiResponse.ok("Active counter services successfully retrieved", queues.activeServices());
	}
}
