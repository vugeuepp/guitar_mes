package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.service.NeckProcessService;
import com.example.guitarmes.service.NeckService;

@Controller
public class NeckProcessViewController {

    private final NeckProcessService neckProcessService;

    private final NeckService neckService;

    public NeckProcessViewController(
            NeckProcessService neckProcessService,
            NeckService neckService) {

        this.neckProcessService =
                neckProcessService;

        this.neckService =
                neckService;
    }

    @GetMapping("/neck-processes/start/view")
    public String showStartForm(
            @RequestParam Long neckId,
            Model model) {

        model.addAttribute(
                "selectedNeck",
                neckService.getNeckById(neckId));

        model.addAttribute(
                "currentProcess",
                neckProcessService
                        .getCurrentProcess(neckId));

        return "neck-process-start-form";
    }

    @PostMapping("/neck-processes/start")
    public String startProcess(
            @RequestParam Long neckId,
            @RequestParam Long processId,
            @RequestParam String workerName) {

        neckProcessService.startProcess(
                neckId,
                processId,
                workerName);

        return "redirect:/necks/view";
    }

    @GetMapping("/neck-processes/end/view")
    public String showEndForm(
            @RequestParam Long neckId,
            Model model) {

        model.addAttribute(
                "neck",
                neckService.getNeckById(neckId));

        model.addAttribute(
                "runningHistory",
                neckProcessService
                        .getRunningProcess(neckId));

        return "neck-process-end-form";
    }

    @PostMapping("/neck-processes/end")
    public String endProcess(
            @RequestParam Long historyId,
            @RequestParam Long neckId,
            @RequestParam String result,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes) {

        try {

            neckProcessService.endProcess(
                    historyId,
                    result,
                    note);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "ネック工程を終了しました。");

            return "redirect:/necks/view";

        } catch (BusinessException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage());

            return "redirect:/neck-processes/end/view"
                    + "?neckId="
                    + neckId;
        }
    }
    @GetMapping("/necks/{id}/process-history")
    public String showProcessHistory(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "neck",
                neckService.getNeckById(id));

        model.addAttribute(
                "histories",
                neckProcessService
                        .getHistoryResponses(id));

        return "neck-process-history";
    }
}