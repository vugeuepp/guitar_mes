package com.example.guitarmes.master.neck;

public class NeckMasterUpdateRequest {

    private String modelCode;
    private String modelName;
    private String productFamilyCode;
    private String neckType;
    private String neckMaterial;
    private String fingerboardMaterial;
    private Integer fretCount;
    private String scale;

    public NeckMasterUpdateRequest() {
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getProductFamilyCode() {
        return productFamilyCode;
    }

    public void setProductFamilyCode(String productFamilyCode) {
        this.productFamilyCode = productFamilyCode;
    }

    public String getNeckType() {
        return neckType;
    }

    public void setNeckType(String neckType) {
        this.neckType = neckType;
    }

    public String getNeckMaterial() {
        return neckMaterial;
    }

    public void setNeckMaterial(String neckMaterial) {
        this.neckMaterial = neckMaterial;
    }

    public String getFingerboardMaterial() {
        return fingerboardMaterial;
    }

    public void setFingerboardMaterial(String fingerboardMaterial) {
        this.fingerboardMaterial = fingerboardMaterial;
    }

    public Integer getFretCount() {
        return fretCount;
    }

    public void setFretCount(Integer fretCount) {
        this.fretCount = fretCount;
    }

    public String getScale() {
        return scale;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }
}
