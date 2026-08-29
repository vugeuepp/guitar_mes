package com.example.guitarmes.entity;

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