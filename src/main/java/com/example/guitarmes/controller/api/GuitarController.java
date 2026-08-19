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
import com.example.guitarmes.entity.Product;
import com.example.guitarmes.service.GuitarService;
import com.example.guitarmes.service.ProductService;

@RestController
@RequestMapping("/api/guitars")
public class GuitarController {
	
	private final GuitarService guitarService;
	
	private final ProductService productService;
	
	public GuitarController(GuitarService guitarService, ProductService productService) {
		this.guitarService = guitarService;
		this.productService = productService;
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
		Product product = productService.getProductById(request.getProductId());
		return guitarService.createGuitar(request.getSerialNo(), product);
	}
	
	@PutMapping("/{id}")
	public Guitar updateGuitar(@PathVariable Long id, @RequestBody GuitarUpdateRequest request) {
		return guitarService.updateGuitar(id, request.getCurrentProcess());
	}
}
