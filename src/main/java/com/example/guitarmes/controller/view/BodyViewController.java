package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.service.BodyService;

@Controller
public class BodyViewController {
	private final BodyService bodyService;

	public BodyViewController(BodyService bodyService) {
		this.bodyService = bodyService;
	}
	
	@GetMapping("/bodies/view")
	public String bodyList(Model model) {
		model.addAttribute("bodies", bodyService.getBodies());
		return "body-list";
	}
	
	@GetMapping("/bodies/new")
	public String newBodyForm() {
		return "body-form";
	}
	
	@PostMapping("/bodies/create")
	public String createBody(
			@RequestParam String serialNo,
			@RequestParam String modelName,
			@RequestParam String color) {
		bodyService.createBody(serialNo, modelName, color);
		return "redirect:/bodies/view";
	}
	
}

