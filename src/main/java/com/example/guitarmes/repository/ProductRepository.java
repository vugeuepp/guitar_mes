package com.example.guitarmes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.Product;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    List<Product> findByProductNameContaining(
            String keyword);

    Optional<Product> findByModelNoIgnoreCase(
            String modelNo);

    boolean existsByModelNoIgnoreCase(
            String modelNo);

    boolean
            existsByInternalModelCodeIgnoreCaseAndColorIgnoreCaseAndFingerboardMaterialIgnoreCase(
                    String internalModelCode,
                    String color,
                    String fingerboardMaterial);

    List<Product>
            findByInternalModelCodeIgnoreCaseOrderByColorAscFingerboardMaterialAsc(
                    String internalModelCode);
}