package com.example.guitarmes.master;

public enum FretCountType {

    NINETEEN(19),

    TWENTY(20),

    TWENTY_ONE(21),

    TWENTY_TWO(22),

    TWENTY_FOUR(24);

    private final int count;

    FretCountType(
            int count) {

        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public String getDisplayName() {
        return count + "フレット";
    }
}