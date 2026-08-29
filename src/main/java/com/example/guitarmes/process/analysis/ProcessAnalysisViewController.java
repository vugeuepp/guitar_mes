package com.example.guitarmes.process.analysis;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.guitarmes.body.process.BodyProcessService;
import com.example.guitarmes.neck.process.NeckProcessService;
import com.example.guitarmes.process.ProcessService;

@Controller
public class ProcessAnalysisViewController {

    private final ProcessService processService;
    private final BodyProcessService bodyProcessService;
    private final NeckProcessService neckProcessService;

    public ProcessAnalysisViewController(
            ProcessService processService,
            BodyProcessService bodyProcessService,
            NeckProcessService neckProcessService) {

        this.processService =
                processService;

        this.bodyProcessService =
                bodyProcessService;

        this.neckProcessService =
                neckProcessService;
    }

    @GetMapping("/process-analysis/view")
    public String showProcessAnalysis(
            Model model) {

        model.addAttribute(
                "guitarAverageTimes",
                processService
                        .getAverageProcessTimes());

        model.addAttribute(
                "bodyAverageTimes",
                bodyProcessService
                        .getAverageProcessTimes());

        model.addAttribute(
                "neckAverageTimes",
                neckProcessService
                        .getAverageProcessTimes());

        return "process-analysis";
    }
}