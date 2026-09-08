package com.example.guitarmes.body;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.master.body.BodyMasterService;
import com.example.guitarmes.body.process.BodyProcessService;

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
            @RequestParam(required = false) String serial,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String currentProcess,
            @RequestParam(required = false) String status,
            Model model) {
        var allBodies = bodyService.getBodies();
        var bodies = bodyService.filterBodies(
                allBodies, serial, modelName, currentProcess, status);
        model.addAttribute("bodies", bodies);
        model.addAttribute("bodyProcesses", bodyProcessService.getBodyProcesses());
        model.addAttribute("serial", serial == null ? "" : serial);
        model.addAttribute("modelName", modelName == null ? "" : modelName);
        model.addAttribute("selectedCurrentProcess",
                currentProcess == null ? "" : currentProcess);
        model.addAttribute("selectedStatus", status == null ? "" : status);
        model.addAttribute("filterApplied", bodyService.hasSearchCondition(
                serial, modelName, currentProcess, status));
        model.addAttribute("resultCount", bodies.size());
        return "body-list";
    }

    @GetMapping("/bodies/new")
    public String newBodyForm(Model model) {

        model.addAttribute(
                "bodyMasters",
                bodyMasterService.getBodyMasters());

        return "body-form";
    }

    @PostMapping("/bodies/create")
    public String createBody(@RequestParam Long bodyMasterId) {

        bodyService.createBody(bodyMasterId);

        return "redirect:/bodies/view";
    }
}
