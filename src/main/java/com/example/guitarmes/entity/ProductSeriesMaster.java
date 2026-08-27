package com.example.guitarmes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "m_product_series",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_series_code",
                        columnNames = "series_code")
        })
public class ProductSeriesMaster {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "series_code",
            nullable = false,
            length = 50)
    private String seriesCode;

    @Column(
            name = "series_name",
            nullable = false,
            length = 150)
    private String seriesName;

    @Column(nullable = false)
    private Boolean active = true;

    public ProductSeriesMaster() {
    }

    public ProductSeriesMaster(
            String seriesCode,
            String seriesName,
            Boolean active) {

        this.seriesCode = seriesCode;
        this.seriesName = seriesName;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id) {

        this.id = id;
    }

    public String getSeriesCode() {
        return seriesCode;
    }

    public void setSeriesCode(
            String seriesCode) {

        this.seriesCode = seriesCode;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(
            String seriesName) {

        this.seriesName = seriesName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(
            Boolean active) {

        this.active = active;
    }
}