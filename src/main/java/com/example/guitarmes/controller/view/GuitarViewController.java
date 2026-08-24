package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.dto.AssemblyResponse;
import com.example.guitarmes.entity.ManufacturingProcess;
import com.example.guitarmes.entity.Product;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.service.AssemblyService;
import com.example.guitarmes.service.GuitarService;
import com.example.guitarmes.service.ProcessService;
import com.example.guitarmes.service.ProductService;


@Controller
public class GuitarViewController {
	private final GuitarService guitarService;
	private final ProcessService processService;
	private final AssemblyService assemblyService;
	private final ProductService productService;

	public GuitarViewController(GuitarService guitarService, ProcessService processService,
			AssemblyService assemblyService, ProductService productService) {
		this.guitarService = guitarService;
		this.processService = processService;
		this.assemblyService = assemblyService;
		this.productService = productService;
	}

	@GetMapping("/guitars/view")
	public String guitarList(Model model) {
		model.addAttribute("guitars", guitarService.getGuitarProgressList(processService, assemblyService));
		return "guitar-list";
	}
	
	@GetMapping("/guitars/new")
	public String newGuitarForm(Model model) {
		return "redirect:/production-orders/view";
	}
	
	@PostMapping("/guitars/create")
	public String createGuitar(@RequestParam Long productId) {
		Product product = productService.getProductById(productId);
		guitarService.createGuitar(product);
		return "redirect:/guitars/view";
	}
	
	@GetMapping("/guitars/{id}/view")
	public String guitarDetail(@PathVariable Long id, Model model) {
		AssemblyResponse assembly = assemblyService.getAssemblyByGuitarId(id);
		
		model.addAttribute("guitar", guitarService.getGuitarById(id));
		model.addAttribute("processStatuses", processService.getProcessStatuses(id));
		model.addAttribute("assembly", assembly);
		
		ManufacturingProcess nextProcess = null;
		try {
			nextProcess = processService.getNextAvailableProcess(id);
		} catch (BusinessException e) {
			nextProcess = null;
		}
		model.addAttribute("nextProcess", nextProcess);
		
		boolean hasRunningProcess = processService.hasRunningProcess(id);
		model.addAttribute("hasRunningProcess", hasRunningProcess);
		return "guitar-detail";
	}
}
