package com.example.guitarmes.neck;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.master.neck.NeckMasterService;

@Controller
public class NeckViewController {

    private final NeckService neckService;
    private final NeckMasterService neckMasterService;

    public NeckViewController(
            NeckService neckService,
            NeckMasterService neckMasterService) {

        this.neckService = neckService;
        this.neckMasterService = neckMasterService;
    }

    @GetMapping("/necks/view")
    public String neckList(Model model) {

        model.addAttribute(
                "necks",
                neckService.getNecks());

        return "neck-list";
    }

    @GetMapping("/necks/new")
    public String newNeckForm(Model model) {

        model.addAttribute(
                "neckMasters",
                neckMasterService.getNeckMasters());

        return "neck-form";
    }

    @PostMapping("/necks/create")
    public String createNeck(
            @RequestParam Long neckMasterId) {

        neckService.createNeck(
                neckMasterId);

        return "redirect:/necks/view";
    }
}