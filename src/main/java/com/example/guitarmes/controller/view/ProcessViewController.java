package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.entity.ProcessHistory;
import com.example.guitarmes.repository.ManufacturingProcessRepository;
import com.example.guitarmes.service.GuitarService;
import com.example.guitarmes.service.ProcessService;


@Controller
public class ProcessViewController {
	private final ProcessService processService;
	private final GuitarService guitarService;
	private final ManufacturingProcessRepository processRepository;
	
	public ProcessViewController(
			ProcessService processService, 
			GuitarService guitarService,
			ManufacturingProcessRepository processRepository) {
		
		this.processService = processService;
		this.guitarService = guitarService;
		this.processRepository = processRepository;
	}
	
	@GetMapping("/processes/start/view")
	public String processStartForm(@RequestParam(required = false)Long guitarId, Model model) {
		if(guitarId != null) {
			model.addAttribute("selectedGuitar", guitarService.getGuitarById(guitarId));
			model.addAttribute("nextProcess", processService.getNextAvailableProcess(guitarId));
		} else {
			model.addAttribute("guitars", guitarService.getGuitars());
			model.addAttribute("processes", processRepository.findAllByOrderByProcessOrderAsc());
		}
		return "process-start-form";
	}
	
	@PostMapping("/processes/start")
	public String startProcess(
			@RequestParam Long guitarId,
			@RequestParam Long processId,
			@RequestParam String workerName) {
		processService.startProcess(guitarId, processId, workerName);
		return "redirect:/guitars/" + guitarId + "/view";
	}
	
	@GetMapping("/processes/end/view")
	public String processEndForm(@RequestParam(required = false) Long guitarId, Model model) {
		if (guitarId != null) {
			ProcessHistory runningHistory = processService.getRunningProcessByGuitarId(guitarId);
			model.addAttribute("runningHistory", runningHistory);
		} else {			
			model.addAttribute("histories", processService.getRunningProcesses());
		}
		return "process-end-form";
	}
	
	@PostMapping("/processes/end")
	public String endProcess(@RequestParam Long historyId) {
		ProcessHistory history = processService.endProcess(historyId);
		return "redirect:/guitars/" + history.getGuitarId() + "/view";
	}
	
	@GetMapping("/guitars/{id}/history")
	public String guitarHistory(@PathVariable Long id, Model model) {
		model.addAttribute("histories", processService.getHistory(id));
		model.addAttribute("guitarId", id);
		return "history-list";
	}
	
}
