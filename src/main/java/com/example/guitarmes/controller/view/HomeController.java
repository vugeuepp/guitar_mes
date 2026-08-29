package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.guitarmes.body.BodyService;
import com.example.guitarmes.neck.NeckService;
import com.example.guitarmes.service.AssemblyService;
import com.example.guitarmes.service.GuitarService;
import com.example.guitarmes.service.ProcessService;

@Controller
public class HomeController {
	
	private final GuitarService guitarService;
	private final ProcessService processService;
	private final AssemblyService assemblyService;
	private final NeckService neckService;
	private final BodyService bodyService;
	
	public HomeController(GuitarService guitarService, ProcessService processService, AssemblyService assemblyService,
			NeckService neckService, BodyService bodyService) {
		this.guitarService = guitarService;
		this.processService = processService;
		this.assemblyService = assemblyService;
		this.neckService = neckService;
		this.bodyService = bodyService;
	}

	@GetMapping("/")
	public String home(Model model) {
	    model.addAttribute("totalGuitarCount", guitarService.getTotalGuitarCount());
	    model.addAttribute("completedGuitarCount", guitarService.getCompletedGuitarCount());
	    model.addAttribute("inProgressGuitarCount", guitarService.getInProgressGuitarCount());
	    model.addAttribute("processCounts", guitarService.getProcessCounts());
	    model.addAttribute("guitars",guitarService.getGuitarProgressList(processService, assemblyService));
	    model.addAttribute("runningProcessCount", processService.getRunningProcesses().size());
	    model.addAttribute("completionRate", guitarService.getCompletionRate());
	    model.addAttribute("availableNeckCount", neckService.getAvailableNeckCount());
	    model.addAttribute("availableBodyCount", bodyService.getAvailableBodyCount());
	    model.addAttribute("averageProcessTimes", processService.getAverageProcessTimes());
	    model.addAttribute("bodyStatusCounts", bodyService.getStatusCounts());
	    model.addAttribute("neckStatusCounts", neckService.getStatusCounts());
	    return "home";
	}
}
