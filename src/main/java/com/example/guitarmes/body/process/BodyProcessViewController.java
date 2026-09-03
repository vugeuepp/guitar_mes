package com.example.guitarmes.body.process;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.guitarmes.body.BodyService;
import com.example.guitarmes.exception.BusinessException;

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
        model.addAttribute("runningHistory", bodyProcessService.getRunningProcessResponse(bodyId));
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

    @PostMapping("/body-processes/bulk/start")
    public String startProcesses(@RequestParam(required = false) java.util.List<Long> bodyIds, @RequestParam Long processId, @RequestParam String workerName, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try { int count = bodyProcessService.startProcesses(bodyIds, processId, workerName).size(); redirectAttributes.addFlashAttribute("successMessage", count + "件のボディ工程を一括開始しました。"); }
        catch (com.example.guitarmes.exception.BusinessException e) { redirectAttributes.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/bodies/view";
    }
    @GetMapping("/body-processes/bulk/end/view")
    public String showBulkEndForm(Model model) {
        model.addAttribute("histories", bodyProcessService.getRunningProcessResponses());
        model.addAttribute("processes", bodyProcessService.getBodyProcesses());
        return "body-process-bulk-end-form";
    }
    @PostMapping("/body-processes/bulk/end")
    public String endProcesses(
            @RequestParam(required = false)
            java.util.List<Long> historyIds,
            @RequestParam String result,
            @RequestParam(required = false)
            String note,
            RedirectAttributes redirectAttributes) {

        try {
            int count = bodyProcessService
                    .endProcesses(
                            historyIds,
                            result,
                            note)
                    .size();

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    count
                    + "件のボディ工程を"
                    + "一括終了しました。");

            return "redirect:/bodies/view";

        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());

            return "redirect:/body-processes/bulk/end/view";
        }
    }
}
