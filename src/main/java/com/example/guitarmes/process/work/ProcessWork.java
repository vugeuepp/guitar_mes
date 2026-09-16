package com.example.guitarmes.process.work;

import java.time.LocalDateTime;

import com.example.guitarmes.process.ProcessHistory;
import com.example.guitarmes.product.parts.BridgeType;
import com.example.guitarmes.product.parts.TunerMountingType;
import com.example.guitarmes.product.parts.TunerLayout;
import com.example.guitarmes.product.parts.JackMountingType;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 1回の工程履歴に属する開始時仕様。snapshotの業務上の更新は行わない。 */
@Entity
@Table(name = "t_process_work", uniqueConstraints = @UniqueConstraint(
        name = "uk_process_work_process_history", columnNames = "process_history_id"))
public class ProcessWork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "process_history_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_process_work_process_history"))
    private ProcessHistory processHistory;

    @Enumerated(EnumType.STRING)
    @Column(name = "bridge_type", length = 32, nullable = false)
    private BridgeType bridgeType;

    @Column(name = "bridge_model", length = 255)
    private String bridgeModel;

    @Column(name = "requires_stud_hole_expansion", nullable = false)
    private Boolean requiresStudHoleExpansion;

    @Column(name = "tuner_model", length = 255, nullable = false)
    private String tunerModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "tuner_mounting_type", length = 32, nullable = false)
    private TunerMountingType tunerMountingType;

    @Column(name = "tuner_bush_required", nullable = false)
    private Boolean tunerBushRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "tuner_layout", length = 32, nullable = false)
    private TunerLayout tunerLayout;

    @Column(name = "pickup_layout", length = 255, nullable = false)
    private String pickupLayout;

    @Column(name = "selector_positions", nullable = false)
    private Integer selectorPositions;

    @Column(name = "control_layout", length = 255, nullable = false)
    private String controlLayout;

    @Enumerated(EnumType.STRING)
    @Column(name = "jack_mounting_type", length = 32, nullable = false)
    private JackMountingType jackMountingType;

    @Column(name = "string_maker", length = 150)
    private String stringMaker;

    @Column(name = "string_model", length = 255, nullable = false)
    private String stringModel;

    @Column(name = "string_gauge", length = 100, nullable = false)
    private String stringGauge;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ProcessWork() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProcessHistory getProcessHistory() {
        return processHistory;
    }

    public void setProcessHistory(ProcessHistory processHistory) {
        this.processHistory = processHistory;
    }

    public BridgeType getBridgeType() {
        return bridgeType;
    }

    public void setBridgeType(BridgeType bridgeType) {
        this.bridgeType = bridgeType;
    }

    public String getBridgeModel() {
        return bridgeModel;
    }

    public void setBridgeModel(String bridgeModel) {
        this.bridgeModel = bridgeModel;
    }

    public Boolean getRequiresStudHoleExpansion() {
        return requiresStudHoleExpansion;
    }

    public void setRequiresStudHoleExpansion(Boolean requiresStudHoleExpansion) {
        this.requiresStudHoleExpansion = requiresStudHoleExpansion;
    }

    public String getTunerModel() {
        return tunerModel;
    }

    public void setTunerModel(String tunerModel) {
        this.tunerModel = tunerModel;
    }

    public TunerMountingType getTunerMountingType() {
        return tunerMountingType;
    }

    public void setTunerMountingType(TunerMountingType tunerMountingType) {
        this.tunerMountingType = tunerMountingType;
    }

    public Boolean getTunerBushRequired() {
        return tunerBushRequired;
    }

    public void setTunerBushRequired(Boolean tunerBushRequired) {
        this.tunerBushRequired = tunerBushRequired;
    }

    public TunerLayout getTunerLayout() {
        return tunerLayout;
    }

    public void setTunerLayout(TunerLayout tunerLayout) {
        this.tunerLayout = tunerLayout;
    }

    public String getPickupLayout() {
        return pickupLayout;
    }

    public void setPickupLayout(String pickupLayout) {
        this.pickupLayout = pickupLayout;
    }

    public Integer getSelectorPositions() {
        return selectorPositions;
    }

    public void setSelectorPositions(Integer selectorPositions) {
        this.selectorPositions = selectorPositions;
    }

    public String getControlLayout() {
        return controlLayout;
    }

    public void setControlLayout(String controlLayout) {
        this.controlLayout = controlLayout;
    }

    public JackMountingType getJackMountingType() {
        return jackMountingType;
    }

    public void setJackMountingType(JackMountingType jackMountingType) {
        this.jackMountingType = jackMountingType;
    }

    public String getStringMaker() {
        return stringMaker;
    }

    public void setStringMaker(String stringMaker) {
        this.stringMaker = stringMaker;
    }

    public String getStringModel() {
        return stringModel;
    }

    public void setStringModel(String stringModel) {
        this.stringModel = stringModel;
    }

    public String getStringGauge() {
        return stringGauge;
    }

    public void setStringGauge(String stringGauge) {
        this.stringGauge = stringGauge;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
