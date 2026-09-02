package com.example.guitarmes.neck.process;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.neck.NeckService;

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

    @PostMapping("/neck-processes/bulk/start")
    public String startProcesses(
            @RequestParam(required = false) java.util.List<Long> neckIds,
            @RequestParam Long processId,
            @RequestParam String workerName,
            RedirectAttributes redirectAttributes) {
        try {
            int count = neckProcessService
                    .startProcesses(neckIds, processId, workerName).size();
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    count + "件のネック工程を一括開始しました。");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }
        return "redirect:/necks/view";
    }

    @GetMapping("/neck-processes/bulk/end/view")
    public String showBulkEndForm(Model model) {
        model.addAttribute(
                "histories",
                neckProcessService.getRunningProcesses());
        model.addAttribute(
                "processes",
                neckProcessService.getNeckProcesses());
        return "neck-process-bulk-end-form";
    }

    @PostMapping("/neck-processes/bulk/end")
    public String endProcesses(
            @RequestParam(required = false)
            java.util.List<Long> historyIds,
            @RequestParam String result,
            @RequestParam(required = false)
            String note,
            RedirectAttributes redirectAttributes) {

        try {
            int count = neckProcessService
                    .endProcesses(
                            historyIds,
                            result,
                            note)
                    .size();

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    count
                    + "件のネック工程を"
                    + "一括終了しました。");

            return "redirect:/necks/view";

        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());

            return "redirect:/neck-processes/bulk/end/view";
        }
    }

}
