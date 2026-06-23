package com.hospital.queue.exception;

import com.hospital.queue.dto.ErrorResponse;
import java.util.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	@ExceptionHandler(BadRequestException.class)
	ResponseEntity<ErrorResponse> bad(BadRequestException ex) { return response(HttpStatus.BAD_REQUEST, ex.getMessage()); }
	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ErrorResponse> missing(NotFoundException ex) { return response(HttpStatus.NOT_FOUND, ex.getMessage()); }
	@ExceptionHandler(ConflictException.class)
	ResponseEntity<ErrorResponse> conflict(ConflictException ex) { return response(HttpStatus.CONFLICT, ex.getMessage()); }
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
		String message = errors.values().stream().findFirst().orElse("Please check your information and try again.");
		return ResponseEntity.badRequest().body(new ErrorResponse(false, message, errors));
	}
	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> unreadable() { return response(HttpStatus.BAD_REQUEST, "Please check your information and try again."); }
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErrorResponse> parameter() { return response(HttpStatus.BAD_REQUEST, "Please check your information and try again."); }
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException ex) {
		String cause = ex.getMostSpecificCause().getMessage();
		log.error("Database rejected queue data: {}", cause, ex);
		return response(HttpStatus.CONFLICT,
			"We could not process your request right now. Please try again or ask hospital staff for help.");
	}
	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ErrorResponse> resourceMissing() { return response(HttpStatus.NOT_FOUND, "Resource not found"); }
	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> unexpected() { return response(HttpStatus.INTERNAL_SERVER_ERROR,
		"Something went wrong. Please try again later."); }
	private ResponseEntity<ErrorResponse> response(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(ErrorResponse.of(message));
	}
}
