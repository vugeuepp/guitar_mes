package com.example.guitarmes.product;

public class ProductVariationRequest {

    private String modelNo;

    private String color;

    private String fingerboardMaterial;

    public ProductVariationRequest() {
    }

    public ProductVariationRequest(
            String modelNo,
            String color,
            String fingerboardMaterial) {

        this.modelNo = modelNo;
        this.color = color;
        this.fingerboardMaterial =
                fingerboardMaterial;
    }

    public String getModelNo() {
        return modelNo;
    }

    public void setModelNo(
            String modelNo) {

        this.modelNo = modelNo;
    }

    public String getColor() {
        return color;
    }

    public void setColor(
            String color) {

        this.color = color;
    }

    public String getFingerboardMaterial() {
        return fingerboardMaterial;
    }

    public void setFingerboardMaterial(
            String fingerboardMaterial) {

        this.fingerboardMaterial =
                fingerboardMaterial;
    }
}