package com.example.guitarmes.controller.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.guitarmes.dto.GuitarCreateRequest;
import com.example.guitarmes.dto.GuitarUpdateRequest;
import com.example.guitarmes.entity.Guitar;
import com.example.guitarmes.service.GuitarService;

@RestController
@RequestMapping("/api/guitars")
public class GuitarController {
	
	private final GuitarService guitarService;
	
	public GuitarController(GuitarService guitarService) {
		this.guitarService = guitarService;
	}
	
	@GetMapping
	public List<Guitar> getGuitars() {
		return guitarService.getGuitars();
	}
	
	@GetMapping("/{id}")
	public Guitar getGuitarById(@PathVariable Long id) {
		return guitarService.getGuitarById(id);
	}
	
	@PostMapping
	public Guitar createGuitar(@RequestBody GuitarCreateRequest request) {
		return guitarService.createGuitar(request.getSerialNo(), request.getModelName(), request.getCurrentProcess());
	}
	
	@PutMapping("/{id}")
	public Guitar updateGuitar(@PathVariable Long id, @RequestBody GuitarUpdateRequest request) {
		return guitarService.updateGuitar(id, request.getCurrentProcess());
	}
}
