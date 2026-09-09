package com.example.guitarmes.body;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.body.process.BodyProcessService;
import com.example.guitarmes.master.body.BodyMasterService;

@Controller
public class BodyViewController {
    private final BodyService bodyService;
    private final BodyMasterService bodyMasterService;
    private final BodyProcessService bodyProcessService;

    public BodyViewController(
            BodyService bodyService,
            BodyMasterService bodyMasterService,
            BodyProcessService bodyProcessService) {
        this.bodyService = bodyService;
        this.bodyMasterService = bodyMasterService;
        this.bodyProcessService = bodyProcessService;
    }

    @GetMapping("/bodies/view")
    public String bodyList(
            @RequestParam(defaultValue = "active") String category,
            @RequestParam(required = false) String serial,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String currentProcess,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        String selectedCategory = bodyService.normalizeCategory(category);
        var result = bodyService.searchBodiesPaged(selectedCategory, serial, modelName, currentProcess, status, page);

        model.addAttribute("category", selectedCategory);
        model.addAttribute("activeCount", bodyService.countCategory("active"));
        model.addAttribute("attentionCount", bodyService.countCategory("attention"));
        model.addAttribute("passedCount", bodyService.countCategory("passed"));
        model.addAttribute("bodies", result.getContent());
        model.addAttribute("bodyProcesses", bodyProcessService.getBodyProcesses());
        model.addAttribute("serial", serial == null ? "" : serial);
        model.addAttribute("modelName", modelName == null ? "" : modelName);
        model.addAttribute("selectedCurrentProcess",
                currentProcess == null ? "" : currentProcess);
        model.addAttribute("selectedStatus", status == null ? "" : status);
        model.addAttribute("filterApplied", bodyService.hasSearchCondition(
                serial, modelName, currentProcess, status));
        model.addAttribute("resultCount", result.getTotalElements());
        model.addAttribute("currentPage", result.getNumber());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("pageSize", BodyService.PAGE_SIZE);
        model.addAttribute("hasPrevious", result.hasPrevious());
        model.addAttribute("hasNext", result.hasNext());
        return "body-list";
    }

    @GetMapping("/bodies/new")
    public String newBodyForm(Model model) {
        model.addAttribute("bodyMasters", bodyMasterService.getBodyMasters());
        return "body-form";
    }

    @PostMapping("/bodies/create")
    public String createBody(@RequestParam Long bodyMasterId) {
        bodyService.createBody(bodyMasterId);
        return "redirect:/bodies/view";
    }
}
