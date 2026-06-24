package com.hospital.queue.controller;

import com.hospital.queue.dto.AdminLoginRequest;
import com.hospital.queue.dto.AdminTokenResponse;
import com.hospital.queue.dto.ApiResponse;
import com.hospital.queue.service.JwtService;
import com.hospital.queue.service.StaffAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/admin/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminAuthApiController {
	private final StaffAuthService authService;
	private final JwtService jwtService;

	public AdminAuthApiController(StaffAuthService authService, JwtService jwtService) {
		this.authService = authService;
		this.jwtService = jwtService;
	}

	@PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponse<AdminTokenResponse>> login(@Valid @RequestBody AdminLoginRequest request) {
		return authService.authenticateUser(request.username(), request.password())
				.map(user -> ResponseEntity.ok(ApiResponse.ok("Login successful",
						new AdminTokenResponse(jwtService.issue(user.getUsername(), user.getRole()),
								"Bearer", jwtService.expirationSeconds()))))
				.orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new ApiResponse<>(false, "Invalid admin ID or password.", null)));
	}
}
