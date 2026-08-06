package com.example.guitarmes.controller.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.guitarmes.dto.NeckCreateRequest;
import com.example.guitarmes.entity.Neck;
import com.example.guitarmes.service.NeckService;

@RestController
@RequestMapping("/api/necks")
public class NeckController {
	private final NeckService neckService;
	
	public NeckController(NeckService neckService) {
		this.neckService = neckService;
	}
	
	@GetMapping
	public List<Neck> getNecks() {
		return neckService.getNecks();
	}
	
	@GetMapping("/{id}")
	public Neck getNeckById(@PathVariable Long id) {
		return neckService.getNeckById(id);
	}
	
	@PostMapping
	public Neck createNeck(@RequestBody NeckCreateRequest request) {
		return neckService.createNeck(
				request.getSerialNo(), 
				request.getModelName(), 
				request.getCurrentProcess(), 
				request.getStatus());
	}
}
