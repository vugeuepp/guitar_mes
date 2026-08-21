package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.guitarmes.entity.NeckMaster;
import com.example.guitarmes.service.NeckMasterService;

@Controller
public class NeckMasterController {

    private final NeckMasterService neckMasterService;

    public NeckMasterController(
            NeckMasterService neckMasterService) {

        this.neckMasterService = neckMasterService;
    }

    @GetMapping("/neck-masters/view")
    public String showNeckMasterList(
            Model model) {

        model.addAttribute(
                "neckMasters",
                neckMasterService.getNeckMasters());

        return "neck-master-list";
    }

    @GetMapping("/neck-masters/new")
    public String showNeckMasterForm() {

        return "neck-master-form";
    }

    @PostMapping("/neck-masters/create")
    public String createNeckMaster(
            NeckMaster neckMaster) {

        neckMasterService.createNeckMaster(neckMaster);

        return "redirect:/neck-masters/view";
    }

    @GetMapping("/neck-masters/{id}/view")
    public String showNeckMasterDetail(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "neckMaster",
                neckMasterService.getNeckMasterById(id));

        return "neck-master-detail";
    }
}