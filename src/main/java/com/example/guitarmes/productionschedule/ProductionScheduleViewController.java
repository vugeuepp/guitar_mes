package com.example.guitarmes.productionschedule;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.productionorder.ProductionOrderService;

@Controller
public class ProductionScheduleViewController {

    private final ProductionScheduleService productionScheduleService;
    private final ProductionOrderService productionOrderService;

    public ProductionScheduleViewController(
            ProductionScheduleService productionScheduleService,
            ProductionOrderService productionOrderService) {

        this.productionScheduleService = productionScheduleService;
        this.productionOrderService = productionOrderService;
    }

    @GetMapping(
            "/production-orders/{productionOrderId}/schedules/new")
    public String showCreateForm(
            @PathVariable Long productionOrderId,
            Model model) {

        addCreateFormAttributes(
                productionOrderId,
                new ProductionScheduleCreateRequest(),
                model);

        return "production-schedule-form";
    }

    @PostMapping(
            "/production-orders/{productionOrderId}/schedules/create")
    public String create(
            @PathVariable Long productionOrderId,
            @ModelAttribute("request")
            ProductionScheduleCreateRequest request,
            Model model) {

        try {
            productionScheduleService.createProductionSchedule(
                    productionOrderId,
                    request.getScheduleDate(),
                    request.getPlannedQuantity());

            return "redirect:/production-orders/"
                    + productionOrderId
                    + "/view";
        } catch (BusinessException exception) {
            addCreateFormAttributes(
                    productionOrderId,
                    request,
                    model);
            model.addAttribute(
                    "errorMessage",
                    exception.getMessage());

            return "production-schedule-form";
        }
    }

    private void addCreateFormAttributes(
            Long productionOrderId,
            ProductionScheduleCreateRequest request,
            Model model) {

        model.addAttribute(
                "order",
                productionOrderService
                        .getProductionOrderById(productionOrderId));
        model.addAttribute("request", request);
        model.addAttribute(
                "allocatedQuantity",
                productionScheduleService
                        .getAllocatedQuantity(productionOrderId));
        model.addAttribute(
                "unallocatedQuantity",
                productionScheduleService
                        .getUnallocatedQuantity(productionOrderId));
    }
}
