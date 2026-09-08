
package com.example.guitarmes.guitar;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.assembly.AssemblyResponse;
import com.example.guitarmes.assembly.AssemblyService;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.process.ManufacturingProcess;
import com.example.guitarmes.process.ProcessService;


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
	public String guitarList(
            @RequestParam(defaultValue = "active") String category,
            @RequestParam(required = false) String serial,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String currentProcess,
            @RequestParam(required = false) String status,
            Model model) {
        var allGuitars = guitarService.getGuitarProgressList(
                processService,
                assemblyService);
        String selectedCategory = guitarService.normalizeCategory(category);
        var activeGuitars = guitarService.filterByCategory(allGuitars, "active");
        var completedGuitars = guitarService.filterByCategory(allGuitars, "completed");
        var categoryGuitars = "completed".equals(selectedCategory) ? completedGuitars : activeGuitars;
        String effectiveStatus = "completed".equals(selectedCategory) ? "" : status;
        model.addAttribute("category", selectedCategory);
        model.addAttribute("activeCount", activeGuitars.size());
        model.addAttribute("completedCount", completedGuitars.size());
        var guitars = guitarService.filterGuitarProgressList(
                categoryGuitars,
                serial,
                product,
                currentProcess,
                effectiveStatus);
        model.addAttribute("guitars", guitars);
        model.addAttribute("processes", processService.getAvailableGuitarProcesses());
        model.addAttribute("productOptions", guitarService.getProductOptions(allGuitars));
        model.addAttribute("serial", serial == null ? "" : serial);
        model.addAttribute("selectedProduct", product == null ? "" : product);
        model.addAttribute("selectedCurrentProcess", currentProcess == null ? "" : currentProcess);
        model.addAttribute("selectedStatus", effectiveStatus == null ? "" : effectiveStatus);
        model.addAttribute("filterApplied", guitarService.hasSearchCondition(
                serial,
                product,
                currentProcess,
                effectiveStatus));
        model.addAttribute("resultCount", guitars.size());
		return "guitar-list";
	}
	
	@GetMapping("/guitars/new")
	public String newGuitarForm(Model model) {
		return "redirect:/production-orders/view";
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
