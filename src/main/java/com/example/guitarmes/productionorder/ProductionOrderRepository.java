package com.example.guitarmes.productionorder;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionOrderRepository
        extends JpaRepository<ProductionOrder, Long>, ProductionOrderSearchRepository {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from ProductionOrder e where e.id = :id")
    Optional<ProductionOrder> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);


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
