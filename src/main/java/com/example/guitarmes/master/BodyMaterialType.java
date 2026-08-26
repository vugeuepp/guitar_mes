package com.example.guitarmes.master;

public enum BodyMaterialType {

    ALDER("Alder"),

    ASH("Ash"),

    BASSWOOD("Basswood"),

    MAHOGANY("Mahogany"),

    MAPLE("Maple"),

    POPLAR("Poplar"),

    OTHER("Other");

    private final String label;

    BodyMaterialType(
            String label) {

        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
