package com.example.guitarmes.product;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.guitar.GuitarService;
import com.example.guitarmes.master.BodyMaterialType;
import com.example.guitarmes.master.FingerboardMaterialType;
import com.example.guitarmes.master.FretCountType;
import com.example.guitarmes.master.NeckMaterialType;
import com.example.guitarmes.master.ScaleLengthType;
import com.example.guitarmes.master.instrumenttype.InstrumentTypeMasterService;
import com.example.guitarmes.master.productseries.ProductSeriesMasterService;
import com.example.guitarmes.product.image.ProductImageService;

@Controller
public class ProductViewController {

    private final ProductService productService;

    private final GuitarService guitarService;

    private final ProductSeriesMasterService
            productSeriesMasterService;

    private final InstrumentTypeMasterService
            instrumentTypeMasterService;
    private final ProductImageService productImageService;

    public ProductViewController(
            ProductService productService,
            GuitarService guitarService,
            ProductSeriesMasterService
                    productSeriesMasterService,
            InstrumentTypeMasterService
                    instrumentTypeMasterService,
            ProductImageService productImageService) {

        this.productService = productService;
        this.guitarService = guitarService;
        this.productSeriesMasterService =
                productSeriesMasterService;
        this.instrumentTypeMasterService =
                instrumentTypeMasterService;
        this.productImageService = productImageService;
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

        addNewProductFormOptions(
                model);

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

    @PostMapping("/products/{id}/image")
    public String uploadProductImage(
            @PathVariable Long id,
            @RequestParam("imageFile")
            MultipartFile imageFile,
            Model model) {

        try {
            productImageService.saveProductImage(
                    id,
                    imageFile);
            return "redirect:/products/"
                    + id
                    + "/view";
        } catch (BusinessException exception) {
            addProductDetailAttributes(id, model);
            model.addAttribute(
                    "imageErrorMessage",
                    exception.getMessage());
            return "product-detail";
        }
    }

    @PostMapping("/products/{id}/image/delete")
    public String deleteProductImage(
            @PathVariable Long id) {

        productImageService.deleteProductImage(id);
        return "redirect:/products/"
                + id
                + "/view";
    }

    @GetMapping("/products/{id}/edit")
    public String editProductForm(
            @PathVariable Long id,
            Model model) {

        ProductUpdateRequest request =
                productService.getProductUpdateRequest(
                        id);

        model.addAttribute(
                "request",
                request);
        model.addAttribute(
                "productId",
                id);

        addEditProductFormOptions(
                model,
                request.getProductSeries(),
                request.getInstrumentType());

        return "product-edit-form";
    }

    @PostMapping("/products/{id}/edit")
    public String updateProduct(
            @PathVariable Long id,
            @ModelAttribute("request")
            ProductUpdateRequest request,
            Model model) {

        try {
            productService.updateProduct(
                    id,
                    request);

            return "redirect:/products/"
                    + id
                    + "/view";

        } catch (BusinessException exception) {

            model.addAttribute(
                    "productId",
                    id);

            model.addAttribute(
                    "errorMessage",
                    exception.getMessage());

            addEditProductFormOptions(
                    model,
                    request.getProductSeries(),
                    request.getInstrumentType());

            return "product-edit-form";
        }
    }

    private void addProductDetailAttributes(
            Long id,
            Model model) {

        model.addAttribute(
                "product",
                productService.getProductById(id));
        model.addAttribute(
                "guitars",
                guitarService.getGuitarsByProductId(id));
    }

    private void addNewProductFormOptions(
            Model model) {

        model.addAttribute(
                "productSeriesList",
                productSeriesMasterService
                        .getActiveProductSeriesMasters());

        model.addAttribute(
                "instrumentTypeList",
                instrumentTypeMasterService
                        .getActiveInstrumentTypeMasters());

        addCommonProductFormOptions(
                model);
    }

    private void addEditProductFormOptions(
            Model model,
            String currentSeriesCode,
            String currentInstrumentCode) {

        model.addAttribute(
                "productSeriesList",
                productSeriesMasterService
                        .getProductSeriesMastersForEdit(
                                currentSeriesCode));

        model.addAttribute(
                "instrumentTypeList",
                instrumentTypeMasterService
                        .getInstrumentTypeMastersForEdit(
                                currentInstrumentCode));

        addCommonProductFormOptions(
                model);
    }

    private void addCommonProductFormOptions(
            Model model) {

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