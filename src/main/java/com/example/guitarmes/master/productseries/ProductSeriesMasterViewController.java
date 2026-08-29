package com.example.guitarmes.master.productseries;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.guitarmes.exception.BusinessException;

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
    @GetMapping("/product-series/{id}/edit")
    public String editProductSeriesForm(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "request",
                productSeriesMasterService
                        .getProductSeriesMasterById(id));
        model.addAttribute("productSeriesId", id);
        return "product-series-edit-form";
    }

    @PostMapping("/product-series/{id}/edit")
    public String updateProductSeries(
            @PathVariable Long id,
            @ModelAttribute("request")
            ProductSeriesMaster request,
            Model model) {

        try {
            productSeriesMasterService
                    .updateProductSeriesMaster(id, request);
            return "redirect:/product-series/view";
        } catch (BusinessException exception) {
            request.setId(id);
            model.addAttribute("productSeriesId", id);
            model.addAttribute(
                    "errorMessage",
                    exception.getMessage());
            return "product-series-edit-form";
        }
    }

    @PostMapping("/product-series/{id}/toggle-active")
    public String toggleProductSeriesActive(
            @PathVariable Long id) {

        productSeriesMasterService
                .toggleProductSeriesMasterActive(id);
        return "redirect:/product-series/view";
    }

}