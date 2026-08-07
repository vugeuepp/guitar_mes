package com.example.guitarmes.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.guitarmes.entity.Product;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {

        return productRepository.findById(id).orElseThrow(
                        () -> new NotFoundException("指定された製品が存在しません。"));
    }
    
    public Product createProduct(
            String modelNo,
            String productName,
            String color,
            String bodyMaterial,
            String neckMaterial,
            String fingerboardMaterial,
            String pickupLayout,
            Integer fretCount,
            String scale) {

        Product product = new Product(
                modelNo,
                productName,
                color,
                bodyMaterial,
                neckMaterial,
                fingerboardMaterial,
                pickupLayout,
                fretCount,
                scale);

        return productRepository.save(product);
    }
}