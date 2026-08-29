package com.example.guitarmes.productionorder;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.product.ProductService;
import com.example.guitarmes.service.GuitarService;

@Controller
public class ProductionOrderViewController {

    private final ProductionOrderService productionOrderService;
    private final ProductService productService;
    private final GuitarService guitarService;

    public ProductionOrderViewController(
            ProductionOrderService productionOrderService,
            ProductService productService,
            GuitarService guitarService) {
        this.productionOrderService = productionOrderService;
        this.productService = productService;
        this.guitarService = guitarService;
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
        model.addAttribute(
                "request",
                new ProductionOrderCreateRequest());
        return "production-order-form";
    }

    @PostMapping("/production-orders/create")
    public String create(ProductionOrderCreateRequest request) {
        productionOrderService.createProductionOrder(
                request.getProductId(),
                request.getPlannedQuantity(),
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
    }
}
