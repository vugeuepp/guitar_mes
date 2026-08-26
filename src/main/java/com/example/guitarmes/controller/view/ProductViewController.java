package com.example.guitarmes.controller.view;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.dto.ProductVariationCreateRequest;
import com.example.guitarmes.dto.ProductVariationRequest;
import com.example.guitarmes.master.BodyMaterialType;
import com.example.guitarmes.master.FingerboardMaterialType;
import com.example.guitarmes.master.FretCountType;
import com.example.guitarmes.master.InstrumentType;
import com.example.guitarmes.master.NeckMaterialType;
import com.example.guitarmes.master.ProductSeries;
import com.example.guitarmes.master.ScaleLengthType;
import com.example.guitarmes.service.GuitarService;
import com.example.guitarmes.service.ProductService;

@Controller
public class ProductViewController {

    private final ProductService productService;

    private final GuitarService guitarService;

    public ProductViewController(
            ProductService productService,
            GuitarService guitarService) {

        this.productService = productService;
        this.guitarService = guitarService;
    }

    @GetMapping("/products/view")
    public String productList(
            @RequestParam(required = false)
            String keyword,
            Model model) {

        model.addAttribute(
                "products",
                productService.searchProducts(keyword));

        model.addAttribute(
                "keyword",
                keyword);

        return "product-list";
    }

    @GetMapping("/products/new")
    public String newProductForm(
            Model model) {

        ProductVariationCreateRequest request =
                new ProductVariationCreateRequest();

        request.setVariations(
                List.of(
                        new ProductVariationRequest()));

        model.addAttribute(
                "request",
                request);

        addProductFormOptions(model);

        return "product-form";
    }

    @PostMapping("/products/create")
    public String createProduct(
            @ModelAttribute("request")
            ProductVariationCreateRequest request) {

        productService.createProductVariations(
                request);

        return "redirect:/products/view";
    }

    @GetMapping("/products/{id}/view")
    public String productDetail(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "product",
                productService.getProductById(id));

        model.addAttribute(
                "guitars",
                guitarService.getGuitarsByProductId(id));

        return "product-detail";
    }

    private void addProductFormOptions(
            Model model) {

        model.addAttribute(
                "productSeriesList",
                ProductSeries.values());

        model.addAttribute(
                "instrumentTypeList",
                InstrumentType.values());

        model.addAttribute(
                "bodyMaterialTypeList",
                BodyMaterialType.values());

        model.addAttribute(
                "neckMaterialTypeList",
                NeckMaterialType.values());

        model.addAttribute(
                "fingerboardMaterialTypeList",
                FingerboardMaterialType.values());

        model.addAttribute(
                "fretCountTypeList",
                FretCountType.values());

        model.addAttribute(
                "scaleLengthTypeList",
                ScaleLengthType.values());
    }
}