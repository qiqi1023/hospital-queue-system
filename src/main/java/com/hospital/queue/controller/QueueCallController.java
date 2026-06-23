package com.hospital.queue.controller;
import com.hospital.queue.dto.*;
import com.hospital.queue.service.QueueService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping(value="/api/queueCalls", produces=MediaType.APPLICATION_JSON_VALUE)
public class QueueCallController {
	private final QueueService service;
	public QueueCallController(QueueService service) { this.service=service; }
	@PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<QueueResponse> call(@Valid @RequestBody CallRequest request) {
		return ApiResponse.ok("Patient called", service.callNext(request));
	}
}
