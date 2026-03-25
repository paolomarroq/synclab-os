package com.synclabos.producerconsumer;

public enum NumberType {
    PRIME("PRIMOS"),
    EVEN("PARES"),
    ODD("IMPARES");

    private final String label;

    NumberType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
