package com.hospital.queue.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
	@GetMapping("/staff")
	public String staffPage() {
		return "forward:/staff.html";
	}
}
