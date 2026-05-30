package com.hospital.queue.controller;

import com.hospital.queue.constant.ApiRoutes;
import com.hospital.queue.dto.CallNextRequest;
import com.hospital.queue.dto.QueueTicketResponse;
import com.hospital.queue.dto.TakeQueueRequest;
import com.hospital.queue.dto.UpdateTicketStatusRequest;
import com.hospital.queue.service.QueueService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	@GetMapping
	public List<QueueTicketResponse> getTickets(
			@RequestParam(required = false) String department,
			@RequestParam(required = false) String status) {
		return queueService.getTickets(department, status);
	}

	@PutMapping(ApiRoutes.NEXT_CALL)
	public QueueTicketResponse callNext(@Valid @RequestBody CallNextRequest request) {
		return queueService.callNext(request);
	}

	@PutMapping(ApiRoutes.QUEUE_NUMBER_PATH + ApiRoutes.STATUS)
	public QueueTicketResponse updateStatus(
			@PathVariable String queueNumber,
			@Valid @RequestBody UpdateTicketStatusRequest request) {
		return queueService.updateStatus(queueNumber, request);
	}

	@PutMapping(ApiRoutes.QUEUE_NUMBER_PATH + ApiRoutes.CANCEL)
	public QueueTicketResponse cancelTicket(@PathVariable String queueNumber) {
		return queueService.cancelTicket(queueNumber);
	}

	@GetMapping(ApiRoutes.QUEUE_NUMBER_PATH)
	public QueueTicketResponse getTicket(@PathVariable String queueNumber) {
		return queueService.getTicket(queueNumber);
	}
}
