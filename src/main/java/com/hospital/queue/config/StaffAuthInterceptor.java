package com.hospital.queue.config;

import com.hospital.queue.service.StaffAuthService;
import com.hospital.queue.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class StaffAuthInterceptor implements HandlerInterceptor {
	private final JwtService jwtService;

	public StaffAuthInterceptor(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if ("POST".equals(request.getMethod()) && request.getRequestURI().endsWith("/api/queueTickets")) {
			return true;
		}
		if (Boolean.TRUE.equals(request.getSession(false) == null ? null
				: request.getSession(false).getAttribute(StaffAuthService.SESSION_ATTRIBUTE))) {
			return true;
		}
		String authorization = request.getHeader("Authorization");
		if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)
				&& jwtService.verify(authorization.substring(7).trim()).isPresent()) {
			return true;
		}

		if (request.getRequestURI().contains("/api/")) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write("{\"success\":false,\"message\":\"Admin login required.\"}");
		}
		else {
			response.sendRedirect(request.getContextPath() + "/admin/login");
		}
		return false;
	}
}
