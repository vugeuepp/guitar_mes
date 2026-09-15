package com.example.guitarmes.product.parts;

/** Product IDはServiceの引数で指定する。Spec IDや関連Productを入力させない。 */
public class ProductPartsSpecRequest {

    private BridgeType bridgeType;

    private String bridgeModel;

    private Boolean requiresStudHoleExpansion;

    private String tunerModel;

    private TunerMountingType tunerMountingType;

    private Boolean tunerBushRequired;

    private TunerLayout tunerLayout;

    private Integer selectorPositions;

    private String controlLayout;

    private JackMountingType jackMountingType;

    private String stringMaker;

    private String stringModel;

    private String stringGauge;

    public boolean isEmpty() {
        return bridgeType == null
                && (bridgeModel == null || bridgeModel.isBlank())
                && requiresStudHoleExpansion == null
                && (tunerModel == null || tunerModel.isBlank())
                && tunerMountingType == null
                && tunerBushRequired == null
                && tunerLayout == null
                && selectorPositions == null
                && (controlLayout == null || controlLayout.isBlank())
                && jackMountingType == null
                && (stringMaker == null || stringMaker.isBlank())
                && (stringModel == null || stringModel.isBlank())
                && (stringGauge == null || stringGauge.isBlank());
    }

    public static ProductPartsSpecRequest from(ProductPartsSpec spec) {
        ProductPartsSpecRequest request = new ProductPartsSpecRequest();
        request.setBridgeType(spec.getBridgeType());
        request.setBridgeModel(spec.getBridgeModel());
        request.setRequiresStudHoleExpansion(spec.getRequiresStudHoleExpansion());
        request.setTunerModel(spec.getTunerModel());
        request.setTunerMountingType(spec.getTunerMountingType());
        request.setTunerBushRequired(spec.getTunerBushRequired());
        request.setTunerLayout(spec.getTunerLayout());
        request.setSelectorPositions(spec.getSelectorPositions());
        request.setControlLayout(spec.getControlLayout());
        request.setJackMountingType(spec.getJackMountingType());
        request.setStringMaker(spec.getStringMaker());
        request.setStringModel(spec.getStringModel());
        request.setStringGauge(spec.getStringGauge());
        return request;
    }

    public BridgeType getBridgeType() {
        return bridgeType;
    }

    public void setBridgeType(
            BridgeType bridgeType) {

        this.bridgeType = bridgeType;
    }

    public String getBridgeModel() {
        return bridgeModel;
    }

    public void setBridgeModel(
            String bridgeModel) {

        this.bridgeModel = bridgeModel;
    }

    public Boolean getRequiresStudHoleExpansion() {
        return requiresStudHoleExpansion;
    }

    public void setRequiresStudHoleExpansion(
            Boolean requiresStudHoleExpansion) {

        this.requiresStudHoleExpansion = requiresStudHoleExpansion;
    }

    public String getTunerModel() {
        return tunerModel;
    }

    public void setTunerModel(
            String tunerModel) {

        this.tunerModel = tunerModel;
    }

    public TunerMountingType getTunerMountingType() {
        return tunerMountingType;
    }

    public void setTunerMountingType(
            TunerMountingType tunerMountingType) {

        this.tunerMountingType = tunerMountingType;
    }

    public Boolean getTunerBushRequired() {
        return tunerBushRequired;
    }

    public void setTunerBushRequired(
            Boolean tunerBushRequired) {

        this.tunerBushRequired = tunerBushRequired;
    }

    public TunerLayout getTunerLayout() {
        return tunerLayout;
    }

    public void setTunerLayout(
            TunerLayout tunerLayout) {

        this.tunerLayout = tunerLayout;
    }

    public Integer getSelectorPositions() {
        return selectorPositions;
    }

    public void setSelectorPositions(
            Integer selectorPositions) {

        this.selectorPositions = selectorPositions;
    }

    public String getControlLayout() {
        return controlLayout;
    }

    public void setControlLayout(
            String controlLayout) {

        this.controlLayout = controlLayout;
    }

    public JackMountingType getJackMountingType() {
        return jackMountingType;
    }

    public void setJackMountingType(
            JackMountingType jackMountingType) {

        this.jackMountingType = jackMountingType;
    }

    public String getStringMaker() {
        return stringMaker;
    }

    public void setStringMaker(
            String stringMaker) {

        this.stringMaker = stringMaker;
    }

    public String getStringModel() {
        return stringModel;
    }

    public void setStringModel(
            String stringModel) {

        this.stringModel = stringModel;
    }

    public String getStringGauge() {
        return stringGauge;
    }

    public void setStringGauge(
            String stringGauge) {

        this.stringGauge = stringGauge;
    }
}
