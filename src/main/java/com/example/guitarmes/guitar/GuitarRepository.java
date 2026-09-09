

package com.example.guitarmes.guitar;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GuitarRepository
        extends JpaRepository<Guitar, Long>, GuitarSearchRepository {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Guitar e where e.id = :id")
    Optional<Guitar> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);


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
