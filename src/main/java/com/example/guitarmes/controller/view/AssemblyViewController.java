package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.service.AssemblyService;
import com.example.guitarmes.service.BodyService;
import com.example.guitarmes.service.GuitarService;
import com.example.guitarmes.service.NeckService;


@Controller
public class AssemblyViewController {
	private final AssemblyService assemblyService;
	private final GuitarService guitarService;
	private final NeckService neckService;
	private final BodyService bodyService;
	
	public AssemblyViewController(
			AssemblyService assemblyService,
			GuitarService guitarService,
			NeckService neckService,
			BodyService bodyService){
		this.assemblyService = assemblyService;
		this.guitarService = guitarService;
		this.neckService = neckService;
		this.bodyService = bodyService;
	}
	
	@GetMapping("/assemblies/view")
	public String assemblyList(Model model) {
		model.addAttribute("assemblies", assemblyService.getAssemblies());
		return "assembly-list";
	}
	
	@GetMapping("/assemblies/new")
	public String newAssemblyForm(@RequestParam(required = false)Long guitarId, Model model) {
	    model.addAttribute("necks", neckService.getAvailableNecks());
	    model.addAttribute("bodies", bodyService.getAvailableBodies());

	    if (guitarId != null) {
	        model.addAttribute("selectedGuitar", guitarService.getGuitarById(guitarId));
	    } else {
	        model.addAttribute("guitars", guitarService.getGuitars());
	    }
	    return "assembly-form";
	}
	
	@PostMapping("/assemblies/create")
	public String createAssembly(
			@RequestParam Long guitarId,
			@RequestParam Long neckId,
			@RequestParam Long bodyId,
			@RequestParam String workerName) {
		assemblyService.createAssembly(guitarId, neckId, bodyId, workerName);
		
		return "redirect:/guitars/" + guitarId + "/view";
	}
	
	@GetMapping("/assemblies/{id}/view")
	public String assemblyDetail(@PathVariable Long id, Model model) {
		model.addAttribute("assembly", assemblyService.getAssemblyById(id));
		return "assembly-detail";
	}
}

