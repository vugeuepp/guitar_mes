package com.example.guitarmes.guitar;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.Transient;
import jakarta.persistence.Column;


import com.example.guitarmes.product.Product;
import com.example.guitarmes.productionorder.ProductionOrder;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_guitar")
public class Guitar {

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

    private String serialNo;

    private String currentProcess;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    /*
     * このギターがどの生産計画から
     * 生成された個体かを保持する。
     *
     * 移行期間中は、既存のGuitarデータを考慮して
     * nullableのままにする。
     */
    @ManyToOne
    @JoinColumn(name = "production_order_id")
    private ProductionOrder productionOrder;

    public Guitar() {
    }

    public Guitar(
            String serialNo,
            String currentProcess) {

        this.serialNo = serialNo;
        this.currentProcess = currentProcess;
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id) {

        this.id = id;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(
            String serialNo) {

        this.serialNo = serialNo;
    }

    public String getCurrentProcess() {
        return currentProcess;
    }

    public void setCurrentProcess(
            String currentProcess) {

        this.currentProcess = currentProcess;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(
            Product product) {

        this.product = product;
    }

    public ProductionOrder getProductionOrder() {
        return productionOrder;
    }

    public void setProductionOrder(
            ProductionOrder productionOrder) {

        this.productionOrder =
                productionOrder;
    }
}