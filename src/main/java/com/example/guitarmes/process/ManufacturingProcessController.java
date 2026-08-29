package com.example.guitarmes.process;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/processes")
public class ManufacturingProcessController {
	private final ManufacturingProcessRepository manufacturingProcessRepository;
	
	public ManufacturingProcessController(ManufacturingProcessRepository processRepository) {
		this.manufacturingProcessRepository = processRepository;
	}
	
	@GetMapping
	public List<ManufacturingProcess> getProcesses() {
		return manufacturingProcessRepository.findAll();
	}
}
