package com.hospital.queue.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.hospital.queue.service.RefDataService;

@Controller
public class PageController {
	private final RefDataService refs;

	public PageController(RefDataService refs) {
		this.refs = refs;
	}

	@GetMapping("/")
	public String home() {
		return "redirect:/customer";
	}

	@GetMapping({ "/index", "/index.html" })
	public String legacyCustomerPage() {
		return "redirect:/customer";
	}

	@GetMapping("/customer")
	public String patientPage(Model model) {
		model.addAttribute("departments", refs.departments());
		model.addAttribute("phoneCodes", refs.phoneCodes());
		return "index";
	}

	@GetMapping({ "/staff", "/staff.html" })
	public String legacyAdminPage() {
		return "redirect:/admin";
	}

	@GetMapping("/admin")
	public String staffPage(Model model) {
		model.addAttribute("departments", refs.departments());
		return "staff";
	}
}
