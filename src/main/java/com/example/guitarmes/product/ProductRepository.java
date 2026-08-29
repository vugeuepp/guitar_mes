package com.example.guitarmes.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    List<Product> findByProductNameContaining(
            String keyword);

    Optional<Product> findByModelNoIgnoreCase(
            String modelNo);

    boolean existsByModelNoIgnoreCase(
            String modelNo);

    boolean existsByModelNoIgnoreCaseAndIdNot(
            String modelNo,
            Long id);

    boolean
            existsByInternalModelCodeIgnoreCaseAndColorIgnoreCaseAndFingerboardMaterialIgnoreCase(
                    String internalModelCode,
                    String color,
                    String fingerboardMaterial);

    boolean
            existsByInternalModelCodeIgnoreCaseAndColorIgnoreCaseAndFingerboardMaterialIgnoreCaseAndIdNot(
                    String internalModelCode,
                    String color,
                    String fingerboardMaterial,
                    Long id);

    List<Product>
            findByInternalModelCodeIgnoreCaseOrderByColorAscFingerboardMaterialAsc(
                    String internalModelCode);

    boolean existsByInternalModelCodeEndingWithIgnoreCase(
            String instrumentCodeSuffix);
}