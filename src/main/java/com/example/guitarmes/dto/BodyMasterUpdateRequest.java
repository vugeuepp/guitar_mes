package com.example.guitarmes.dto;

public class BodyMasterUpdateRequest {

    private String modelCode;
    private String modelName;
    private String productFamilyCode;
    private String bodyType;
    private String material;
    private String color;

    public BodyMasterUpdateRequest() {
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
        this.productFamilyCode = productFamilyCode;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(
            String bodyType) {
        this.bodyType = bodyType;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(
            String material) {
        this.material = material;
    }

    public String getColor() {
        return color;
    }

    public void setColor(
            String color) {
        this.color = color;
    }
}
