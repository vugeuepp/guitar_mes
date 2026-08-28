package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.guitarmes.entity.InstrumentTypeMaster;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.service.InstrumentTypeMasterService;

@Controller
public class InstrumentTypeMasterViewController {

    private final InstrumentTypeMasterService
            instrumentTypeMasterService;

    public InstrumentTypeMasterViewController(
            InstrumentTypeMasterService
                    instrumentTypeMasterService) {

        this.instrumentTypeMasterService =
                instrumentTypeMasterService;
    }

    @GetMapping("/instrument-types/view")
    public String instrumentTypeList(
            Model model) {

        model.addAttribute(
                "instrumentTypeMasters",
                instrumentTypeMasterService
                        .getInstrumentTypeMasters());

        return "instrument-type-list";
    }

    @GetMapping("/instrument-types/new")
    public String newInstrumentTypeForm(
            Model model) {

        model.addAttribute(
                "request",
                new InstrumentTypeMaster());

        return "instrument-type-form";
    }

    @PostMapping("/instrument-types/create")
    public String createInstrumentType(
            @ModelAttribute("request")
            InstrumentTypeMaster request,
            Model model) {

        try {
            instrumentTypeMasterService
                    .createInstrumentTypeMaster(
                            request);

            return "redirect:/instrument-types/view";

        } catch (BusinessException exception) {
            model.addAttribute(
                    "errorMessage",
                    exception.getMessage());

            return "instrument-type-form";
        }
    }
}
