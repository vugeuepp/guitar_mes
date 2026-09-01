package com.example.guitarmes.productionschedule;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionScheduleRepository
        extends JpaRepository<ProductionSchedule, Long> {

    List<ProductionSchedule>
            findByProductionOrderIdOrderByScheduleDateAsc(
                    Long productionOrderId);

    boolean existsByProductionOrderIdAndScheduleDate(
            Long productionOrderId,
            LocalDate scheduleDate);

    boolean existsByProductionOrderIdAndScheduleDateAndIdNot(
            Long productionOrderId,
            LocalDate scheduleDate,
            Long id);

    @Query("""
            SELECT COALESCE(SUM(schedule.plannedQuantity), 0)
            FROM ProductionSchedule schedule
            WHERE schedule.productionOrder.id = :productionOrderId
              AND schedule.status <> :cancelledStatus
            """)
    Long sumAllocatedQuantity(
            @Param("productionOrderId") Long productionOrderId,
            @Param("cancelledStatus") String cancelledStatus);

    @Query("""
            SELECT COALESCE(SUM(schedule.plannedQuantity), 0)
            FROM ProductionSchedule schedule
            WHERE schedule.productionOrder.id = :productionOrderId
              AND schedule.status <> :cancelledStatus
              AND schedule.id <> :excludedId
            """)
    Long sumAllocatedQuantityExcludingId(
            @Param("productionOrderId") Long productionOrderId,
            @Param("cancelledStatus") String cancelledStatus,
            @Param("excludedId") Long excludedId);
}
