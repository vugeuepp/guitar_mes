package com.example.guitarmes.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

public class ProductionOrderUpdateRequest {

    private Long productId;
    private Integer plannedQuantity;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate plannedStartDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    public ProductionOrderUpdateRequest() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(Integer plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
    }

    public LocalDate getPlannedStartDate() {
        return plannedStartDate;
    }

    public void setPlannedStartDate(LocalDate plannedStartDate) {
        this.plannedStartDate = plannedStartDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
