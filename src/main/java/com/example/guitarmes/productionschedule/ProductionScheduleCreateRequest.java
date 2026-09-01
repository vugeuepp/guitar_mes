package com.example.guitarmes.productionschedule;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

public class ProductionScheduleCreateRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate scheduleDate;

    private Integer plannedQuantity;

    public ProductionScheduleCreateRequest() {
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(
            LocalDate scheduleDate) {

        this.scheduleDate = scheduleDate;
    }

    public Integer getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(
            Integer plannedQuantity) {

        this.plannedQuantity = plannedQuantity;
    }
}
