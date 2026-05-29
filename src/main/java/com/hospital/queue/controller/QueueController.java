package com.hospital.queue.controller;

import com.hospital.queue.constant.ApiRoutes;
import com.hospital.queue.dto.QueueTicketResponse;
import com.hospital.queue.dto.TakeQueueRequest;
import com.hospital.queue.service.QueueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.QUEUES)
public class QueueController {
	private final QueueService queueService;

	public QueueController(QueueService queueService) {
		this.queueService = queueService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public QueueTicketResponse takeQueue(@Valid @RequestBody TakeQueueRequest request) {
		return queueService.takeQueue(request);
	}

	@GetMapping(ApiRoutes.QUEUE_NUMBER_PATH)
	public QueueTicketResponse getTicket(@PathVariable String queueNumber) {
		return queueService.getTicket(queueNumber);
	}
}
