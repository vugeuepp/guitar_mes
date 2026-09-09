package com.example.guitarmes.productionorder;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.Transient;


import java.time.LocalDate;
import java.time.YearMonth;

import com.example.guitarmes.common.YearMonthDateConverter;
import com.example.guitarmes.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_production_order")
public class ProductionOrder {

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Transient
    private LocalDateTime persistedUpdatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        // Serviceが設定したイベント時刻は保持し、通常編集だけ現在時刻にする。
        // 読込時の値と比較するため、detached Entityのmergeでも明示時刻を保持できる。
        if (Objects.equals(updatedAt, persistedUpdatedAt)) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    public void rememberUpdatedAt() {
        persistedUpdatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime value) { completedAt = value; }


    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            unique = true)
    private String orderNo;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer plannedQuantity;

    @Column(nullable = false)
    private Integer startedQuantity;

    @Column(nullable = false)
    private Integer completedQuantity;

    @Convert(converter = YearMonthDateConverter.class)
    @Column(
            name = "plan_month",
            nullable = false)
    private YearMonth planMonth;

    private LocalDate plannedStartDate;

    private LocalDate dueDate;

    @Column(nullable = false)
    private String status;

    public ProductionOrder() {
    }

    public ProductionOrder(
            String orderNo,
            Product product,
            Integer plannedQuantity,
            YearMonth planMonth,
            LocalDate plannedStartDate,
            LocalDate dueDate,
            String status) {

        this.orderNo = orderNo;
        this.product = product;
        this.plannedQuantity =
                plannedQuantity;

        this.startedQuantity = 0;
        this.completedQuantity = 0;

        this.planMonth = planMonth;

        this.plannedStartDate =
                plannedStartDate;

        this.dueDate = dueDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id) {

        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(
            String orderNo) {

        this.orderNo = orderNo;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(
            Product product) {

        this.product = product;
    }

    public Integer getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(
            Integer plannedQuantity) {

        this.plannedQuantity =
                plannedQuantity;
    }

    public Integer getStartedQuantity() {
        return startedQuantity;
    }

    public void setStartedQuantity(
            Integer startedQuantity) {

        this.startedQuantity =
                startedQuantity;
    }

    public Integer getCompletedQuantity() {
        return completedQuantity;
    }

    public void setCompletedQuantity(
            Integer completedQuantity) {

        this.completedQuantity =
                completedQuantity;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status) {

        this.status = status;
    }
}