package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.guitarmes.dto
        .ProductionOrderCreateRequest;
import com.example.guitarmes.service.GuitarService;
import com.example.guitarmes.service
        .ProductService;
import com.example.guitarmes.service
        .ProductionOrderService;

@Controller
public class ProductionOrderViewController {

    private final ProductionOrderService productionOrderService;

    private final ProductService productService;
    
    private final GuitarService guitarService;

    public ProductionOrderViewController(
            ProductionOrderService productionOrderService,
            ProductService productService,
            GuitarService guitarService) {

        this.productionOrderService =
                productionOrderService;

        this.productService =
                productService;

        this.guitarService =
                guitarService;
    }

    @GetMapping(
            "/production-orders/view")
    public String showList(
            Model model) {

        model.addAttribute(
                "orders",
                productionOrderService
                        .getProductionOrders());

        return "production-order-list";
    }

    @GetMapping(
            "/production-orders/new")
    public String showCreateForm(
            Model model) {

        model.addAttribute(
                "products",
                productService
                        .getProducts());

        model.addAttribute(
                "request",
                new ProductionOrderCreateRequest());

        return "production-order-form";
    }

    @PostMapping(
            "/production-orders/create")
    public String create(
            ProductionOrderCreateRequest request) {

        productionOrderService
                .createProductionOrder(
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

        model.addAttribute(
                "order",
                productionOrderService
                        .getProductionOrderById(id));

        model.addAttribute(
                "guitars",
                guitarService
                        .getGuitarsByProductionOrderId(id));

        return "production-order-detail";
    }
    
    
}