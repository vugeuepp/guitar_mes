package com.example.guitarmes.process;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.guitarmes.exception.BusinessException;

import com.example.guitarmes.guitar.GuitarService;
import com.example.guitarmes.process.common.ProcessTargetConstants;


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
	public String processStartForm(
	        @RequestParam(required = false) Long guitarId, Model model) {

	    if (guitarId != null) {
	        model.addAttribute("selectedGuitar", guitarService.getGuitarById(guitarId));

	        model.addAttribute("nextProcess", processService.getNextAvailableProcess(guitarId));

	    } else {
	        model.addAttribute("guitars", guitarService.getGuitars());

	        model.addAttribute("processes", processRepository.findByTargetTypeOrderByProcessOrderAsc(ProcessTargetConstants.GUITAR));
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
	public String processEndForm(
	        @RequestParam(required = false) Long guitarId,
	        Model model) {

	    boolean guitarSpecified = guitarId != null;

	    model.addAttribute("guitarSpecified", guitarSpecified);

	    if (guitarSpecified) {

	        ProcessRunningResponse runningHistory = processService.getRunningProcessResponseByGuitarId(guitarId);

	        model.addAttribute("runningHistory", runningHistory);

	    } else {
	    	
	        model.addAttribute("histories", processService.getRunningProcessResponses());
            model.addAttribute("processes", processService.getAvailableGuitarProcesses());
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
	

    @PostMapping("/processes/bulk/start")
    public String startProcesses(
            @RequestParam(required = false) java.util.List<Long> guitarIds,
            @RequestParam Long processId,
            @RequestParam String workerName,
            RedirectAttributes redirectAttributes) {
        try {
            int count = processService.startProcesses(
                    guitarIds, processId, workerName).size();
            redirectAttributes.addFlashAttribute(
                    "successMessage", count + "件の工程を一括開始しました。");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", exception.getMessage());
        }
        return "redirect:/guitars/view";
    }

    @PostMapping("/processes/bulk/end")
    public String endProcesses(
            @RequestParam(required = false) java.util.List<Long> historyIds,
            RedirectAttributes redirectAttributes) {
        try {
            int count = processService.endProcesses(historyIds).size();
            redirectAttributes.addFlashAttribute(
                    "successMessage", count + "件の工程を一括終了しました。");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", exception.getMessage());
        }
        return "redirect:/guitars/view";
    }
}
