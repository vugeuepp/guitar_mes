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
        name = "m_instrument_type",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_instrument_type_code",
                        columnNames = "instrument_code")
        })
public class InstrumentTypeMaster {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "instrument_code",
            nullable = false,
            length = 20)
    private String instrumentCode;

    @Column(
            name = "instrument_name",
            nullable = false,
            length = 100)
    private String instrumentName;

    @Column(
            name = "body_type",
            nullable = false,
            length = 100)
    private String bodyType;

    @Column(
            name = "neck_type",
            nullable = false,
            length = 100)
    private String neckType;

    @Column(nullable = false)
    private Boolean active = true;

    public InstrumentTypeMaster() {
    }

    public InstrumentTypeMaster(
            String instrumentCode,
            String instrumentName,
            String bodyType,
            String neckType,
            Boolean active) {

        this.instrumentCode = instrumentCode;
        this.instrumentName = instrumentName;
        this.bodyType = bodyType;
        this.neckType = neckType;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id) {
        this.id = id;
    }

    public String getInstrumentCode() {
        return instrumentCode;
    }

    public void setInstrumentCode(
            String instrumentCode) {
        this.instrumentCode = instrumentCode;
    }

    public String getInstrumentName() {
        return instrumentName;
    }

    public void setInstrumentName(
            String instrumentName) {
        this.instrumentName = instrumentName;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(
            String bodyType) {
        this.bodyType = bodyType;
    }

    public String getNeckType() {
        return neckType;
    }

    public void setNeckType(
            String neckType) {
        this.neckType = neckType;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(
            Boolean active) {
        this.active = active;
    }
}
