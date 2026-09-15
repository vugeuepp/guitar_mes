package com.example.guitarmes.product.parts;

import com.example.guitarmes.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 製品ごとのパーツ仕様。未登録は正常。作業指示・実績は保持しない。 */
@Entity
@Table(
        name = "m_product_parts_spec",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_parts_spec_product",
                columnNames = "product_id"))
public class ProductPartsSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 片方向とし、既存Productの取得にSpecの存在確認SQLを追加しない。
    // cascade / orphanRemovalによるProductやSpecの暗黙的な削除は行わない。
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_parts_spec_product"))
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "bridge_type", length = 32)
    private BridgeType bridgeType;

    @Column(name = "bridge_model", length = 255)
    private String bridgeModel;

    @Column(name = "requires_stud_hole_expansion")
    private Boolean requiresStudHoleExpansion;

    @Column(name = "tuner_model", length = 255)
    private String tunerModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "tuner_mounting_type", length = 32)
    private TunerMountingType tunerMountingType;

    @Column(name = "tuner_bush_required")
    private Boolean tunerBushRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "tuner_layout", length = 32)
    private TunerLayout tunerLayout;

    @Column(name = "selector_positions")
    private Integer selectorPositions;

    @Column(name = "control_layout", length = 255)
    private String controlLayout;

    @Enumerated(EnumType.STRING)
    @Column(name = "jack_mounting_type", length = 32)
    private JackMountingType jackMountingType;

    @Column(name = "string_maker", length = 150)
    private String stringMaker;

    @Column(name = "string_model", length = 255)
    private String stringModel;

    @Column(name = "string_gauge", length = 100)
    private String stringGauge;

    public ProductPartsSpec() {
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id) {

        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(
            Product product) {

        this.product = product;
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
