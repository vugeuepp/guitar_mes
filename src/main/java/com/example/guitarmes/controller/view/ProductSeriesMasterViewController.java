package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.guitarmes.entity.ProductSeriesMaster;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.service.ProductSeriesMasterService;

@Controller
public class ProductSeriesMasterViewController {

    private final ProductSeriesMasterService
            productSeriesMasterService;

    public ProductSeriesMasterViewController(
            ProductSeriesMasterService
                    productSeriesMasterService) {

        this.productSeriesMasterService =
                productSeriesMasterService;
    }

    @GetMapping("/product-series/view")
    public String productSeriesList(
            Model model) {

        model.addAttribute(
                "productSeriesMasters",
                productSeriesMasterService
                        .getProductSeriesMasters());

        return "product-series-list";
    }

    @GetMapping("/product-series/new")
    public String newProductSeriesForm(
            Model model) {

        model.addAttribute(
                "request",
                new ProductSeriesMaster());

        return "product-series-form";
    }

    @PostMapping("/product-series/create")
    public String createProductSeries(
            @ModelAttribute("request")
            ProductSeriesMaster request,
            Model model) {

        try {
            productSeriesMasterService
                    .createProductSeriesMaster(
                            request);

            return "redirect:/product-series/view";

        } catch (BusinessException exception) {
            model.addAttribute(
                    "errorMessage",
                    exception.getMessage());

            return "product-series-form";
        }
    }
}