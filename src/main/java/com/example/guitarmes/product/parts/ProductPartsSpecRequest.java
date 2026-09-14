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
