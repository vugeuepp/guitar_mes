package com.example.guitarmes.product.parts;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPartsSpecRepository
        extends JpaRepository<ProductPartsSpec, Long> {

    Optional<ProductPartsSpec> findByProductId(Long productId);

    boolean existsByProductId(Long productId);
}
