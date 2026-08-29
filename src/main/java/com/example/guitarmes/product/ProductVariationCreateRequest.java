package com.example.guitarmes.product;

import java.util.ArrayList;
import java.util.List;

public class ProductVariationCreateRequest {

    private String productSeries;

    private String instrumentType;

    private String internalModelCode;

    private String productName;

    private String bodyType;

    private String bodyMaterial;

    private String neckType;

    private String neckMaterial;

    private String pickupLayout;

    private Integer fretCount;

    private String scale;

    private List<ProductVariationRequest> variations =
            new ArrayList<>();

    public ProductVariationCreateRequest() {
    }

    public String getProductSeries() {
        return productSeries;
    }

    public void setProductSeries(
            String productSeries) {

        this.productSeries = productSeries;
    }

    public String getInstrumentType() {
        return instrumentType;
    }

    public void setInstrumentType(
            String instrumentType) {

        this.instrumentType = instrumentType;
    }

    public String getInternalModelCode() {
        return internalModelCode;
    }

    public void setInternalModelCode(
            String internalModelCode) {

        this.internalModelCode =
                internalModelCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(
            String productName) {

        this.productName = productName;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(
            String bodyType) {

        this.bodyType = bodyType;
    }

    public String getBodyMaterial() {
        return bodyMaterial;
    }

    public void setBodyMaterial(
            String bodyMaterial) {

        this.bodyMaterial = bodyMaterial;
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

    public String getPickupLayout() {
        return pickupLayout;
    }

    public void setPickupLayout(
            String pickupLayout) {

        this.pickupLayout = pickupLayout;
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

    public List<ProductVariationRequest>
            getVariations() {

        return variations;
    }

    public void setVariations(
            List<ProductVariationRequest> variations) {

        if (variations == null) {
            this.variations =
                    new ArrayList<>();
            return;
        }

        this.variations = variations;
    }
}