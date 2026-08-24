package com.example.guitarmes.dto;

import java.time.LocalDate;

public class ProductionOrderCreateRequest {

    private Long productId;

    private Integer plannedQuantity;

    private LocalDate plannedStartDate;

    private LocalDate dueDate;

    public ProductionOrderCreateRequest() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(
            Long productId) {

        this.productId = productId;
    }

    public Integer getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(
            Integer plannedQuantity) {

        this.plannedQuantity =
                plannedQuantity;
    }

    public LocalDate getPlannedStartDate() {
        return plannedStartDate;
    }

    public void setPlannedStartDate(
            LocalDate plannedStartDate) {

        this.plannedStartDate =
                plannedStartDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(
            LocalDate dueDate) {

        this.dueDate = dueDate;
    }
}