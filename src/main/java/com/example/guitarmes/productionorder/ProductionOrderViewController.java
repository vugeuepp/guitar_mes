package com.example.guitarmes.productionorder;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.guitar.GuitarService;
import com.example.guitarmes.product.ProductService;
import com.example.guitarmes.productionschedule.ProductionSchedule;
import com.example.guitarmes.productionschedule.ProductionScheduleService;

@Controller
public class ProductionOrderViewController {

    private final ProductionOrderService productionOrderService;
    private final ProductService productService;
    private final GuitarService guitarService;
    private final ProductionScheduleService productionScheduleService;

    public ProductionOrderViewController(
            ProductionOrderService productionOrderService,
            ProductService productService,
            GuitarService guitarService,
            ProductionScheduleService productionScheduleService) {
        this.productionOrderService = productionOrderService;
        this.productService = productService;
        this.guitarService = guitarService;
        this.productionScheduleService = productionScheduleService;
    }

    @GetMapping("/production-orders/view")
    public String showList(Model model) {
        model.addAttribute(
                "orders",
                productionOrderService.getProductionOrders());
        return "production-order-list";
    }

    @GetMapping("/production-orders/new")
    public String showCreateForm(Model model) {
        model.addAttribute("products", productService.getProducts());
        ProductionOrderCreateRequest request =
                new ProductionOrderCreateRequest();
        LocalDate today = LocalDate.now();
        request.setPlanMonth(java.time.YearMonth.from(today));
        request.setPlannedStartDate(today);
        request.setDueDate(today.plusDays(7));
        model.addAttribute("request", request);
        addDateRangeAttributes(model);
        return "production-order-form";
    }

    @PostMapping("/production-orders/create")
    public String create(ProductionOrderCreateRequest request) {
        productionOrderService.createProductionOrder(
                request.getProductId(),
                request.getPlannedQuantity(),
                request.getPlanMonth(),
                request.getPlannedStartDate(),
                request.getDueDate());
        return "redirect:/production-orders/view";
    }

    @GetMapping("/production-orders/{id}/view")
    public String showDetail(
            @PathVariable Long id,
            Model model) {
        addDetailAttributes(id, model);
        return "production-order-detail";
    }

    @GetMapping("/production-orders/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model) {
        model.addAttribute(
                "request",
                productionOrderService
                        .getProductionOrderUpdateRequest(id));
        model.addAttribute(
                "order",
                productionOrderService.getProductionOrderById(id));
        model.addAttribute("products", productService.getProducts());
        addDateRangeAttributes(model);
        return "production-order-edit-form";
    }

    @PostMapping("/production-orders/{id}/edit")
    public String update(
            @PathVariable Long id,
            @ModelAttribute("request")
            ProductionOrderUpdateRequest request,
            Model model) {
        try {
            productionOrderService.updateProductionOrder(id, request);
            return "redirect:/production-orders/" + id + "/view";
        } catch (BusinessException exception) {
            model.addAttribute(
                    "order",
                    productionOrderService.getProductionOrderById(id));
            model.addAttribute("products", productService.getProducts());
            addDateRangeAttributes(model);
            model.addAttribute("errorMessage", exception.getMessage());
            return "production-order-edit-form";
        }
    }

    @PostMapping("/production-orders/{id}/cancel")
    public String cancel(
            @PathVariable Long id,
            Model model) {
        try {
            productionOrderService.cancelProductionOrder(id);
            return "redirect:/production-orders/" + id + "/view";
        } catch (BusinessException exception) {
            addDetailAttributes(id, model);
            model.addAttribute("errorMessage", exception.getMessage());
            return "production-order-detail";
        }
    }

    private void addDetailAttributes(Long id, Model model) {
        model.addAttribute(
                "order",
                productionOrderService.getProductionOrderById(id));
        model.addAttribute(
                "guitars",
                guitarService.getGuitarsByProductionOrderId(id));
        List<ProductionSchedule> productionSchedules =
                productionScheduleService
                        .getProductionSchedulesByOrderId(id);
        model.addAttribute(
                "productionSchedules",
                productionSchedules);
        addIssueAttributes(productionSchedules, model);
        model.addAttribute(
                "allocatedQuantity",
                productionScheduleService.getAllocatedQuantity(id));
        model.addAttribute(
                "unallocatedQuantity",
                productionScheduleService.getUnallocatedQuantity(id));
    }
    private void addIssueAttributes(
            List<ProductionSchedule> productionSchedules,
            Model model) {
        Map<Long, Long> issuedBodyCounts = new LinkedHashMap<>();
        Map<Long, Long> issuedNeckCounts = new LinkedHashMap<>();
        Map<Long, Boolean> componentsIssued = new LinkedHashMap<>();
        Map<Long, Boolean> neckInstallAvailable = new LinkedHashMap<>();
        for (ProductionSchedule schedule : productionSchedules) {
            Long scheduleId = schedule.getId();
            issuedBodyCounts.put(
                    scheduleId,
                    productionScheduleService
                            .getIssuedBodyCount(scheduleId));
            issuedNeckCounts.put(
                    scheduleId,
                    productionScheduleService
                            .getIssuedNeckCount(scheduleId));
            boolean issued = productionScheduleService
                    .isComponentsIssued(schedule);
            componentsIssued.put(scheduleId, issued);
            ProductionOrder order = schedule.getProductionOrder();
            boolean belowOrderLimit = order != null
                    && order.getStartedQuantity() != null
                    && order.getPlannedQuantity() != null
                    && order.getStartedQuantity() < order.getPlannedQuantity();
            neckInstallAvailable.put(
                    scheduleId,
                    issued && belowOrderLimit);
        }
        model.addAttribute("issuedBodyCounts", issuedBodyCounts);
        model.addAttribute("issuedNeckCounts", issuedNeckCounts);
        model.addAttribute("componentsIssued", componentsIssued);
        model.addAttribute("neckInstallAvailable", neckInstallAvailable);
    }


    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(
                LocalDate.class,
                new FlexibleLocalDateEditor());
    }

    private void addDateRangeAttributes(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("minimumPlanDate", today);
        model.addAttribute("maximumPlanDate", today.plusYears(5));
    }

    private static class FlexibleLocalDateEditor
            extends PropertyEditorSupport {
        private static final List<DateTimeFormatter> FORMATTERS = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("uuuu/MM/dd"),
                DateTimeFormatter.BASIC_ISO_DATE);

        @Override
        public void setAsText(String text) {
            if (text == null || text.isBlank()) {
                setValue(null);
                return;
            }
            String value = text.trim();
            for (DateTimeFormatter formatter : FORMATTERS) {
                try {
                    setValue(LocalDate.parse(value, formatter));
                    return;
                } catch (DateTimeParseException ignored) {
                    // 次の対応形式を試す。
                }
            }
            throw new IllegalArgumentException(
                    "日付はyyyy/MM/dd形式で入力してください。");
        }
    }
}
