package com.example.guitarmes.product.parts;

public enum TunerMountingType {

    PRESS_BUSHING("圧入ブッシュ式"),

    NUT_FASTENING("ナット固定式");

    private final String label;

    TunerMountingType(
            String label) {

        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
