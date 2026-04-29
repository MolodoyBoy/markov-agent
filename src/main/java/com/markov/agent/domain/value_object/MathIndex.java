package com.markov.agent.domain.value_object;

public enum MathIndex {

    AVG(1),
    STD_DEV(2);

    private final int index;

    MathIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
