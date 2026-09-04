package com.example.guitarmes.productionschedule;
import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

            return redirectToProductionOrder(productionOrderId);
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

    @GetMapping("/production-schedules/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model) {

        ProductionSchedule productionSchedule =
                productionScheduleService
                        .getProductionScheduleById(id);

        ProductionScheduleUpdateRequest request =
                new ProductionScheduleUpdateRequest();
        request.setScheduleDate(
                productionSchedule.getScheduleDate());
        request.setPlannedQuantity(
                productionSchedule.getPlannedQuantity());

        addEditFormAttributes(
                productionSchedule,
                request,
                model);

        return "production-schedule-edit-form";
    }

    @PostMapping("/production-schedules/{id}/edit")
    public String update(
            @PathVariable Long id,
            @ModelAttribute("request")
            ProductionScheduleUpdateRequest request,
            Model model) {

        ProductionSchedule productionSchedule =
                productionScheduleService
                        .getProductionScheduleById(id);

        try {
            productionScheduleService.updateProductionSchedule(
                    id,
                    request.getScheduleDate(),
                    request.getPlannedQuantity());

            return redirectToProductionOrder(
                    productionSchedule
                            .getProductionOrder()
                            .getId());
        } catch (BusinessException exception) {
            addEditFormAttributes(
                    productionSchedule,
                    request,
                    model);
            model.addAttribute(
                    "errorMessage",
                    exception.getMessage());

            return "production-schedule-edit-form";
        }
    }

    @PostMapping("/production-schedules/{id}/confirm")
    public String confirm(
            @PathVariable Long id,
            Model model) {

        ProductionSchedule productionSchedule =
                productionScheduleService
                        .getProductionScheduleById(id);
        Long productionOrderId = productionSchedule
                .getProductionOrder()
                .getId();

        try {
            productionScheduleService
                    .confirmProductionSchedule(id);

            return redirectToProductionOrder(productionOrderId);
        } catch (BusinessException exception) {
            return returnToProductionOrderDetail(
                    productionOrderId,
                    exception,
                    model);
        }
    }

    @PostMapping("/production-schedules/{id}/issue-components")
    public String issueComponents(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        ProductionSchedule productionSchedule =
                productionScheduleService
                        .getProductionScheduleById(id);
        Long productionOrderId = productionSchedule
                .getProductionOrder()
                .getId();
        try {
            productionScheduleService.issueComponents(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "BodyとNeckを一括発行しました。");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }
        return redirectToProductionOrder(productionOrderId);
    }

    @PostMapping("/production-schedules/{id}/cancel")
    public String cancel(
            @PathVariable Long id,
            Model model) {

        ProductionSchedule productionSchedule =
                productionScheduleService
                        .getProductionScheduleById(id);
        Long productionOrderId = productionSchedule
                .getProductionOrder()
                .getId();

        try {
            productionScheduleService
                    .cancelProductionSchedule(id);

            return redirectToProductionOrder(productionOrderId);
        } catch (BusinessException exception) {
            return returnToProductionOrderDetail(
                    productionOrderId,
                    exception,
                    model);
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
        addAllocationAttributes(productionOrderId, model);
        addScheduleDateRangeAttributes(productionOrderId, model);
    }

    private void addEditFormAttributes(
            ProductionSchedule productionSchedule,
            ProductionScheduleUpdateRequest request,
            Model model) {

        Long productionOrderId = productionSchedule
                .getProductionOrder()
                .getId();

        model.addAttribute(
                "order",
                productionOrderService
                        .getProductionOrderById(productionOrderId));
        model.addAttribute(
                "productionSchedule",
                productionSchedule);
        model.addAttribute("request", request);
        addAllocationAttributes(productionOrderId, model);
        addScheduleDateRangeAttributes(productionOrderId, model);
    }

    private void addAllocationAttributes(
            Long productionOrderId,
            Model model) {

        model.addAttribute(
                "allocatedQuantity",
                productionScheduleService
                        .getAllocatedQuantity(productionOrderId));
        model.addAttribute(
                "unallocatedQuantity",
                productionScheduleService
                        .getUnallocatedQuantity(productionOrderId));
    }

    private void addScheduleDateRangeAttributes(Long productionOrderId, Model model) {
        YearMonth planMonth = productionOrderService
                .getProductionOrderById(productionOrderId)
                .getPlanMonth();
        model.addAttribute("minimumScheduleDate", planMonth.atDay(1));
        model.addAttribute("maximumScheduleDate", planMonth.atEndOfMonth());
    }
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LocalDate.class, new FlexibleLocalDateEditor());
    }
    private static class FlexibleLocalDateEditor extends PropertyEditorSupport {
        private static final List<DateTimeFormatter> FORMATTERS = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("uuuu/MM/dd"),
                DateTimeFormatter.BASIC_ISO_DATE);
        @Override
        public void setAsText(String text) {
            if (text == null || text.isBlank()) { setValue(null); return; }
            for (DateTimeFormatter formatter : FORMATTERS) {
                try { setValue(LocalDate.parse(text.trim(), formatter)); return; }
                catch (DateTimeParseException ignored) { }
            }
            throw new IllegalArgumentException("日付はyyyy/MM/dd形式で入力してください。");
        }
    }

    private String returnToProductionOrderDetail(
            Long productionOrderId,
            BusinessException exception,
            Model model) {

        model.addAttribute(
                "errorMessage",
                exception.getMessage());

        return redirectToProductionOrder(productionOrderId);
    }

    private String redirectToProductionOrder(
            Long productionOrderId) {

        return "redirect:/production-orders/"
                + productionOrderId
                + "/view";
    }
}
