package com.example.guitarmes.master;

public enum NeckMaterialType {

    MAPLE("Maple"),

    ROASTED_MAPLE("Roasted Maple"),

    MAHOGANY("Mahogany"),

    OTHER("Other");

    private final String label;

    NeckMaterialType(
            String label) {

        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}