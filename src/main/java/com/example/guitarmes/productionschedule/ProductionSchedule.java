package com.example.guitarmes.productionschedule;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.guitarmes.productionorder.ProductionOrder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_production_schedule")
public class ProductionSchedule {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "production_order_id",
            nullable = false)
    private ProductionOrder productionOrder;

    @Column(
            name = "schedule_date",
            nullable = false)
    private LocalDate scheduleDate;

    @Column(
            name = "planned_quantity",
            nullable = false)
    private Integer plannedQuantity;

    @Column(
            nullable = false,
            length = 30)
    private String status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false)
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false)
    private LocalDateTime updatedAt;

    public ProductionSchedule() {
    }

    public ProductionSchedule(
            ProductionOrder productionOrder,
            LocalDate scheduleDate,
            Integer plannedQuantity,
            String status) {

        this.productionOrder = productionOrder;
        this.scheduleDate = scheduleDate;
        this.plannedQuantity = plannedQuantity;
        this.status = status;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id) {

        this.id = id;
    }

    public ProductionOrder getProductionOrder() {
        return productionOrder;
    }

    public void setProductionOrder(
            ProductionOrder productionOrder) {

        this.productionOrder = productionOrder;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status) {

        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {

        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt) {

        this.updatedAt = updatedAt;
    }
}
