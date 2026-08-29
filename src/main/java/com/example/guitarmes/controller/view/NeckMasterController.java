package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.guitarmes.dto.NeckMasterUpdateRequest;
import com.example.guitarmes.entity.NeckMaster;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.service.NeckMasterService;

@Controller
public class NeckMasterController {

    private final NeckMasterService neckMasterService;

    public NeckMasterController(
            NeckMasterService neckMasterService) {
        this.neckMasterService = neckMasterService;
    }

    @GetMapping("/neck-masters/view")
    public String showNeckMasterList(Model model) {
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

    @GetMapping("/neck-masters/{id}/edit")
    public String showNeckMasterEditForm(
            @PathVariable Long id,
            Model model) {
        model.addAttribute(
                "request",
                neckMasterService
                        .getNeckMasterUpdateRequest(id));
        model.addAttribute("neckMasterId", id);
        return "neck-master-edit-form";
    }

    @PostMapping("/neck-masters/{id}/edit")
    public String updateNeckMaster(
            @PathVariable Long id,
            @ModelAttribute("request")
            NeckMasterUpdateRequest request,
            Model model) {
        try {
            neckMasterService.updateNeckMaster(id, request);
            return "redirect:/neck-masters/"
                    + id
                    + "/view";
        } catch (BusinessException exception) {
            model.addAttribute("neckMasterId", id);
            model.addAttribute(
                    "errorMessage",
                    exception.getMessage());
            return "neck-master-edit-form";
        }
    }
}
