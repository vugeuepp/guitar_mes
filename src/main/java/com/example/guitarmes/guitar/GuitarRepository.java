package com.example.guitarmes.guitar;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GuitarRepository
        extends JpaRepository<Guitar, Long> {

    List<Guitar> findByProductId(
            Long productId);

    boolean existsByProductId(
            Long productId);

    boolean existsByProductionOrderId(
            Long productionOrderId);

    Optional<Guitar>
            findTopBySerialNoStartingWithOrderBySerialNoDesc(
                    String prefix);

    List<Guitar>
            findByProductionOrderIdOrderByIdAsc(
                    Long productionOrderId);
}