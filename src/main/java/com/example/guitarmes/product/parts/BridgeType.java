package com.example.guitarmes.product.parts;

public enum BridgeType {

    SIX_POINT("6点支持"),

    TWO_POINT("2点支持"),

    FLOYD_ROSE("Floyd Rose");

    private final String label;

    BridgeType(
            String label) {

        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
