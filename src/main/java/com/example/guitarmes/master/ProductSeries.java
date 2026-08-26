package com.example.guitarmes.master;

public enum ProductSeries {

    MADE_IN_JAPAN_HERITAGE_50S(
            "MIJ-HER50",
            "Made in Japan Heritage 50s"),

    MADE_IN_JAPAN_TRADITIONAL_50S(
            "MIJ-TR50",
            "Made in Japan Traditional 50s"),

    MADE_IN_JAPAN_TRADITIONAL_60S(
            "MIJ-TR60",
            "Made in Japan Traditional 60s"),

    MADE_IN_JAPAN_TRADITIONAL_70S(
            "MIJ-TR70",
            "Made in Japan Traditional 70s"),

    MADE_IN_JAPAN_TRADITIONAL_ORIGINAL_50S(
            "MIJ-TR50-ORIGINAL",
            "Made in Japan Traditional Original 50s"),

    MADE_IN_JAPAN_HYBRID_II(
            "MIJ-H2",
            "Made in Japan Hybrid II"),

    OTHER(
            "OTHER",
            "その他");

    private final String code;

    private final String label;

    ProductSeries(
            String code,
            String label) {

        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
