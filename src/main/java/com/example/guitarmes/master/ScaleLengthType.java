package com.example.guitarmes.master;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ScaleLengthType {

    GUITAR_LONG(
            new BigDecimal("25.5"),
            "レギュラースケール"),

    GUITAR_MEDIUM(
            new BigDecimal("24.75"),
            "ミディアムスケール"),

    GUITAR_SHORT(
            new BigDecimal("24"),
            "ショートスケール"),

    GUITAR_MINI(
            new BigDecimal("22.75"),
            "ミニスケール"),

    BASS_LONG(
            new BigDecimal("34"),
            "ベース・ロングスケール"),

    BASS_MEDIUM(
            new BigDecimal("32"),
            "ベース・ミディアムスケール"),

    BASS_SHORT(
            new BigDecimal("30"),
            "ベース・ショートスケール");

    private static final BigDecimal MILLIMETERS_PER_INCH =
            new BigDecimal("25.4");

    private final BigDecimal inches;

    private final String label;

    ScaleLengthType(
            BigDecimal inches,
            String label) {

        this.inches = inches;
        this.label = label;
    }

    public BigDecimal getInches() {
        return inches;
    }

    public String getValue() {
        return inches.stripTrailingZeros()
                .toPlainString();
    }

    public String getLabel() {
        return label;
    }

    public int getMillimeters() {
        return inches
                .multiply(MILLIMETERS_PER_INCH)
                .setScale(
                        0,
                        RoundingMode.HALF_UP)
                .intValue();
    }

    public String getDisplayName() {
        return label
                + " / "
                + getValue()
                + "インチ（約"
                + getMillimeters()
                + "mm）";
    }
}