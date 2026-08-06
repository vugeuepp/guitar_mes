package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.guitarmes.service.AssemblyService;
import com.example.guitarmes.service.GuitarService;
import com.example.guitarmes.service.ProcessService;

@Controller
public class HomeController {
	
	private final GuitarService guitarService;
	private final ProcessService processService;
	private final AssemblyService assemblyService;
	
	public HomeController(
	        GuitarService guitarService,
	        ProcessService processService,
	        AssemblyService assemblyService) {

	    this.guitarService = guitarService;
	    this.processService = processService;
	    this.assemblyService = assemblyService;
	}
	
	@GetMapping("/")
	public String home(Model model) {
	    model.addAttribute("totalGuitarCount", guitarService.getTotalGuitarCount());
	    model.addAttribute("completedGuitarCount", guitarService.getCompletedGuitarCount());
	    model.addAttribute("inProgressGuitarCount", guitarService.getInProgressGuitarCount());
	    model.addAttribute("processCounts", guitarService.getProcessCounts());
	    model.addAttribute("guitars",guitarService.getGuitarProgressList(processService, assemblyService));
	    return "home";
	}
}
