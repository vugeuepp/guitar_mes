package com.example.guitarmes.master;

public enum FingerboardMaterialType {

    MAPLE("Maple"),

    ROSEWOOD("Rosewood"),

    EBONY("Ebony"),

    PAU_FERRO("Pau Ferro"),

    LAUREL("Laurel"),

    OTHER("Other");

    private final String label;

    FingerboardMaterialType(
            String label) {

        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}