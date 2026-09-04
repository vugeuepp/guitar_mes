package com.example.guitarmes.productionorder;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;

public class ProductionOrderCreateRequest {

    private Long productId;

    private Integer plannedQuantity;

    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth planMonth;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate plannedStartDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
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

    public YearMonth getPlanMonth() {
        return planMonth;
    }

    public void setPlanMonth(
            YearMonth planMonth) {

        this.planMonth = planMonth;
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
