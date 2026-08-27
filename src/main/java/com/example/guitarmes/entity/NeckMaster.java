package com.example.guitarmes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "m_neck")
public class NeckMaster {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY)
    private Long id;

    private String modelCode;

    private String modelName;

    @Column(
            name = "product_family_code",
            length = 50)
    private String productFamilyCode;
    
    private String neckType;

    private String neckMaterial;

    private String fingerboardMaterial;

    private Integer fretCount;

    private String scale;

    public NeckMaster() {
    }

    public NeckMaster(
            String modelCode,
            String modelName,
            String neckType,
            String neckMaterial,
            String fingerboardMaterial,
            Integer fretCount,
            String scale) {

        this.modelCode = modelCode;
        this.modelName = modelName;
        this.neckType = neckType;
        this.neckMaterial = neckMaterial;
        this.fingerboardMaterial =
                fingerboardMaterial;
        this.fretCount = fretCount;
        this.scale = scale;
    }

    public Long getId() {
        return id;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(
            String modelCode) {

        this.modelCode = modelCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(
            String modelName) {

        this.modelName = modelName;
    }

    public String getProductFamilyCode() {
        return productFamilyCode;
    }

    public void setProductFamilyCode(
            String productFamilyCode) {

        this.productFamilyCode =
                productFamilyCode;
    }

    public String getNeckType() {
        return neckType;
    }

    public void setNeckType(
            String neckType) {

        this.neckType = neckType;
    }

    public String getNeckMaterial() {
        return neckMaterial;
    }

    public void setNeckMaterial(
            String neckMaterial) {

        this.neckMaterial = neckMaterial;
    }

    public String getFingerboardMaterial() {
        return fingerboardMaterial;
    }

    public void setFingerboardMaterial(
            String fingerboardMaterial) {

        this.fingerboardMaterial =
                fingerboardMaterial;
    }

    public Integer getFretCount() {
        return fretCount;
    }

    public void setFretCount(
            Integer fretCount) {

        this.fretCount = fretCount;
    }

    public String getScale() {
        return scale;
    }

    public void setScale(
            String scale) {

        this.scale = scale;
    }
}