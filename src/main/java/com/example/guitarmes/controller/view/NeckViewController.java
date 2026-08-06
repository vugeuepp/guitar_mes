package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.service.NeckService;

@Controller
public class NeckViewController {
	private final NeckService neckService;

	public NeckViewController(NeckService neckService) {
		this.neckService = neckService;
	}
	
	@GetMapping("/necks/view")
	public String neckList(Model model) {
		model.addAttribute("necks", neckService.getNecks());
		return "neck-list";
	}
	
	@GetMapping("/necks/new")
	public String newNeckForm() {
		return "neck-form";
	}
	
	@PostMapping("/necks/create")
	public String createNeck(
			@RequestParam String serialNo,
			@RequestParam String modelName,
			@RequestParam String currentProcess,
			@RequestParam String status) {
		neckService.createNeck(serialNo, modelName, currentProcess, status);
		return "redirect:/necks/view";
	}
	
}

