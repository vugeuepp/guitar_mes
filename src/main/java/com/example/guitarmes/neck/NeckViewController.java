package com.example.guitarmes.neck;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.master.neck.NeckMasterService;
import com.example.guitarmes.neck.process.NeckProcessService;

@Controller
public class NeckViewController {
    private final NeckService neckService;
    private final NeckMasterService neckMasterService;
    private final NeckProcessService neckProcessService;

    public NeckViewController(NeckService neckService,
            NeckMasterService neckMasterService,
            NeckProcessService neckProcessService) {
        this.neckService = neckService;
        this.neckMasterService = neckMasterService;
        this.neckProcessService = neckProcessService;
    }

    @GetMapping("/necks/view")
    public String neckList(
            @RequestParam(required = false) String serial,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String currentProcess,
            @RequestParam(required = false) String status,
            Model model) {
        var allNecks = neckService.getNecks();
        var necks = neckService.filterNecks(
                allNecks, serial, modelName, currentProcess, status);
        model.addAttribute("necks", necks);
        model.addAttribute("neckProcesses", neckProcessService.getNeckProcesses());
        model.addAttribute("serial", serial == null ? "" : serial);
        model.addAttribute("modelName", modelName == null ? "" : modelName);
        model.addAttribute("selectedCurrentProcess",
                currentProcess == null ? "" : currentProcess);
        model.addAttribute("selectedStatus", status == null ? "" : status);
        model.addAttribute("filterApplied", neckService.hasSearchCondition(
                serial, modelName, currentProcess, status));
        model.addAttribute("resultCount", necks.size());
        return "neck-list";
    }

    @GetMapping("/necks/new")
    public String newNeckForm(Model model) {
        model.addAttribute("neckMasters", neckMasterService.getNeckMasters());
        return "neck-form";
    }

    @PostMapping("/necks/create")
    public String createNeck(@RequestParam Long neckMasterId) {
        neckService.createNeck(neckMasterId);
        return "redirect:/necks/view";
    }
}
