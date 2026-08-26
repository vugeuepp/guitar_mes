package com.example.guitarmes.master;

public enum InstrumentType {

    STRATOCASTER(
            "ST",
            "Stratocaster",
            "Stratocaster",
            "Stratocaster"),

    TELECASTER(
            "TL",
            "Telecaster",
            "Telecaster",
            "Telecaster"),

    TELECASTER_CUSTOM(
            "TLC",
            "Telecaster Custom",
            "Telecaster",
            "Telecaster"),

    JAZZMASTER(
            "JM",
            "Jazzmaster",
            "Jazzmaster",
            "Jazzmaster"),

    JAGUAR(
            "JG",
            "Jaguar",
            "Jaguar",
            "Jaguar"),

    MUSTANG(
            "MG",
            "Mustang",
            "Mustang",
            "Mustang"),

    PRECISION_BASS(
            "PB",
            "Precision Bass",
            "Precision Bass",
            "Precision Bass"),

    JAZZ_BASS(
            "JB",
            "Jazz Bass",
            "Jazz Bass",
            "Jazz Bass"),

    OTHER(
            "OTHER",
            "その他",
            "Other",
            "Other");

    private final String code;

    private final String label;

    private final String bodyType;

    private final String neckType;

    InstrumentType(
            String code,
            String label,
            String bodyType,
            String neckType) {

        this.code = code;
        this.label = label;
        this.bodyType = bodyType;
        this.neckType = neckType;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getBodyType() {
        return bodyType;
    }

    public String getNeckType() {
        return neckType;
    }
}