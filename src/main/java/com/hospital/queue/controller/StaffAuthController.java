package com.hospital.queue.controller;

import com.hospital.queue.service.StaffAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StaffAuthController {
	private final StaffAuthService authService;

	public StaffAuthController(StaffAuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/admin/login")
	public String loginPage(HttpSession session) {
		if (Boolean.TRUE.equals(session.getAttribute(StaffAuthService.SESSION_ATTRIBUTE))) {
			return "redirect:/admin";
		}
		return "staff-login";
	}

	@PostMapping("/admin/login")
	public String login(@RequestParam String username, @RequestParam String password,
			HttpServletRequest request, Model model) {
		if (!authService.authenticate(username.trim(), password)) {
			model.addAttribute("loginError", "Invalid admin ID or password.");
			model.addAttribute("username", username.trim());
			return "staff-login";
		}

		HttpSession session = request.getSession(false);
		if (session == null) {
			session = request.getSession(true);
		}
		else {
			request.changeSessionId();
		}
		session.setAttribute(StaffAuthService.SESSION_ATTRIBUTE, true);
		session.setAttribute(StaffAuthService.SESSION_USERNAME, username.trim());
		return "redirect:/admin";
	}

	@PostMapping("/admin/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/admin/login?logout";
	}
}
