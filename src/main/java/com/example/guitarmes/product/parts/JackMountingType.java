package com.example.guitarmes.product.parts;

public enum JackMountingType {

    BOAT_PLATE("舟形ジャックプレート");

    private final String label;

    JackMountingType(
            String label) {

        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
