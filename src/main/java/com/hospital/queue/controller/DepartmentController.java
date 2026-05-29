package com.hospital.queue.controller;

import com.hospital.queue.constant.ApiRoutes;
import com.hospital.queue.dto.CurrentQueueResponse;
import com.hospital.queue.dto.DepartmentResponse;
import com.hospital.queue.service.QueueService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.DEPARTMENTS)
public class DepartmentController {
	private final QueueService queueService;

	public DepartmentController(QueueService queueService) {
		this.queueService = queueService;
	}

	@GetMapping
	public List<DepartmentResponse> getDepartments() {
		return queueService.getDepartments();
	}

	@GetMapping(ApiRoutes.CURRENT_QUEUES)
	public List<CurrentQueueResponse> getCurrentQueues() {
		return queueService.getCurrentQueues();
	}
}
