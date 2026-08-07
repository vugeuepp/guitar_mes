package com.example.guitarmes.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.service.ProductService;

@Controller
public class ProductViewController {
	private final ProductService productService;
	
	public ProductViewController(ProductService productService) {
		this.productService = productService;
	}
	
	@GetMapping("/products/view")
	public String productList(Model model) {
		model.addAttribute("products", productService.getProducts());
		
		return "product-list";
	}
	
	@GetMapping("/products/new")
	public String newProductForm() {
	    return "product-form";
	}
	
	@PostMapping("/products/create")
	public String createProduct(

	        @RequestParam String modelNo,
	        @RequestParam String productName,
	        @RequestParam String color,
	        @RequestParam String bodyMaterial,
	        @RequestParam String neckMaterial,
	        @RequestParam String fingerboardMaterial,
	        @RequestParam String pickupLayout,
	        @RequestParam Integer fretCount,
	        @RequestParam String scale) {

	    productService.createProduct(
	            modelNo,
	            productName,
	            color,
	            bodyMaterial,
	            neckMaterial,
	            fingerboardMaterial,
	            pickupLayout,
	            fretCount,
	            scale);

	    return "redirect:/products/view";
	}
	
	@GetMapping("/products/{id}/view")
	public String productDetail(
	        @PathVariable Long id,
	        Model model) {

	    model.addAttribute(
	            "product",
	            productService.getProductById(id));

	    return "product-detail";
	}
}
