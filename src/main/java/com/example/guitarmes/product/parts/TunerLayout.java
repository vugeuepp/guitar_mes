package com.example.guitarmes.product.parts;

public enum TunerLayout {

    SIX_IN_LINE("6連");

    private final String label;

    TunerLayout(
            String label) {

        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
