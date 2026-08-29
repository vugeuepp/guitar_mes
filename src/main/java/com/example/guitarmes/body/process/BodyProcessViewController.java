package com.example.guitarmes.body.process;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.body.BodyService;

@Controller
public class BodyProcessViewController {

    private final BodyProcessService bodyProcessService;
    
    private final BodyService bodyService;

    public BodyProcessViewController(BodyProcessService bodyProcessService, BodyService bodyService) {
        this.bodyProcessService = bodyProcessService;
        this.bodyService = bodyService;
    }

    @GetMapping("/body-processes/start/view")
    public String showStartForm(@RequestParam(required = false) Long bodyId, Model model) {
            model.addAttribute("selectedBody", bodyService.getBodyById(bodyId));
            model.addAttribute("currentProcess", bodyProcessService.getCurrentProcess(bodyId));
        return "body-process-start-form";
    }

    @PostMapping("/body-processes/start")
    public String startProcess(
            @RequestParam Long bodyId,
            @RequestParam Long processId,
            @RequestParam String workerName) {
    	
        bodyProcessService.startProcess(bodyId, processId, workerName);
        return "redirect:/bodies/view";
    }

    @GetMapping("/body-processes/end/view")
    public String showEndForm(@RequestParam Long bodyId, Model model) {
        model.addAttribute("body", bodyService.getBodyById(bodyId));
        model.addAttribute("runningHistory", bodyProcessService.getRunningProcess(bodyId));
        return "body-process-end-form";
    }

    @PostMapping("/body-processes/end")
    public String endProcess(
            @RequestParam Long historyId,
            @RequestParam Long bodyId,
            @RequestParam String result,
            @RequestParam(required = false) String note) {

        bodyProcessService.endProcess(historyId, result,note);

        return "redirect:/bodies/view";
    }
    
    @GetMapping("/bodies/{id}/process-history")
    public String showProcessHistory(
            @PathVariable Long id,
            Model model) {

        model.addAttribute("body", bodyService.getBodyById(id));
        model.addAttribute("histories", bodyProcessService.getHistoryResponses(id));

        return "body-process-history";
    }
}