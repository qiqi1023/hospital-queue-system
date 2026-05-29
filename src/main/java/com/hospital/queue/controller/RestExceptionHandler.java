package com.hospital.queue.controller;

import com.hospital.queue.constant.ResponseMessages;
import com.hospital.queue.dto.ErrorResponse;
import com.hospital.queue.service.BusinessRuleException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {
	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
		HttpStatus status = ex.getStatus();
		return ResponseEntity.status(status).body(new ErrorResponse(
			LocalDateTime.now(),
			status.value(),
			status.getReasonPhrase(),
			ex.getMessage(),
			Map.of()
		));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> validationErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(error ->
			validationErrors.put(error.getField(), error.getDefaultMessage())
		);
		HttpStatus status = HttpStatus.BAD_REQUEST;
		return ResponseEntity.badRequest().body(new ErrorResponse(
			LocalDateTime.now(),
			status.value(),
			status.getReasonPhrase(),
			ResponseMessages.REQUEST_VALIDATION_FAILED,
			validationErrors
		));
	}
}
