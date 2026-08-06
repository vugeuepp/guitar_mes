package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.dto.AssemblyResponse;
import com.example.guitarmes.entity.ManufacturingProcess;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.service.AssemblyService;
import com.example.guitarmes.service.GuitarService;
import com.example.guitarmes.service.ProcessService;


@Controller
public class GuitarViewController {
	private final GuitarService guitarService;
	private final ProcessService processService;
	private final AssemblyService assemblyService;

	public GuitarViewController(GuitarService guitarService, ProcessService processService,
			AssemblyService assemblyService) {
		this.guitarService = guitarService;
		this.processService = processService;
		this.assemblyService = assemblyService;
	}

	@GetMapping("/guitars/view")
	public String guitarList(Model model) {
		model.addAttribute("guitars", guitarService.getGuitarProgressList(processService, assemblyService));
		return "guitar-list";
	}
	
	@GetMapping("/guitars/new")
	public String newGuitarForm() {
		return "guitar-form";
	}
	
	@PostMapping("/guitars/create")
	public String createGuitar(
			@RequestParam String serialNo,
			@RequestParam String modelName,
			@RequestParam String currentProcess) {
		guitarService.createGuitar(serialNo, modelName, currentProcess);
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
		
		boolean needAssembly = nextProcess != null 
				&& "ネック取付".equals(nextProcess.getProcessName())
				&& assembly == null;
		model.addAttribute("needAssembly", needAssembly);
		
		boolean hasRunningProcess = processService.hasRunningProcess(id);
		model.addAttribute("hasRunningProcess", hasRunningProcess);
		return "guitar-detail";
	}
}
