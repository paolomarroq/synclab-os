package com.synclabos.diningphilosophers;

public enum PhilosopherState {
    THINKING("Pensando"),
    WAITING("Esperando"),
    EATING("Comiendo");

    private final String label;

    PhilosopherState(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
