package com.example.guitarmes.productionorder;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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

    List<ProductionOrder> findByProductId(
            Long productId);
}
