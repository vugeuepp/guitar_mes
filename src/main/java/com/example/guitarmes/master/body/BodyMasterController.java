package com.example.guitarmes.master.body;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.guitarmes.exception.BusinessException;

@Controller
public class BodyMasterController {

    private final BodyMasterService bodyMasterService;

    public BodyMasterController(
            BodyMasterService bodyMasterService) {
        this.bodyMasterService = bodyMasterService;
    }

    @GetMapping("/body-masters/view")
    public String showBodyMasterList(
            Model model) {
        model.addAttribute(
                "bodyMasters",
                bodyMasterService.getBodyMasters());
        return "body-master-list";
    }

    @GetMapping("/body-masters/new")
    public String showBodyMasterForm() {
        return "body-master-form";
    }

    @PostMapping("/body-masters/create")
    public String createBodyMaster(
            BodyMaster bodyMaster) {
        bodyMasterService.createBodyMaster(bodyMaster);
        return "redirect:/body-masters/view";
    }

    @GetMapping("/body-masters/{id}/view")
    public String showBodyMasterDetail(
            @PathVariable Long id,
            Model model) {
        model.addAttribute(
                "bodyMaster",
                bodyMasterService.getBodyMasterById(id));
        return "body-master-detail";
    }

    @GetMapping("/body-masters/{id}/edit")
    public String showBodyMasterEditForm(
            @PathVariable Long id,
            Model model) {
        model.addAttribute(
                "request",
                bodyMasterService
                        .getBodyMasterUpdateRequest(id));
        model.addAttribute("bodyMasterId", id);
        return "body-master-edit-form";
    }

    @PostMapping("/body-masters/{id}/edit")
    public String updateBodyMaster(
            @PathVariable Long id,
            @ModelAttribute("request")
            BodyMasterUpdateRequest request,
            Model model) {

        try {
            bodyMasterService.updateBodyMaster(
                    id,
                    request);
            return "redirect:/body-masters/"
                    + id
                    + "/view";
        } catch (BusinessException exception) {
            model.addAttribute("bodyMasterId", id);
            model.addAttribute(
                    "errorMessage",
                    exception.getMessage());
            return "body-master-edit-form";
        }
    }
}
