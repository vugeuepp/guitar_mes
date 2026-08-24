package com.example.guitarmes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.ProductionOrder;

public interface ProductionOrderRepository
        extends JpaRepository<ProductionOrder, Long> {

    Optional<ProductionOrder>
            findTopByOrderNoStartingWithOrderByOrderNoDesc(
                    String prefix);

    List<ProductionOrder>
            findAllByOrderByIdDesc();

    List<ProductionOrder>
            findByStatusOrderByIdDesc(
                    String status);
}