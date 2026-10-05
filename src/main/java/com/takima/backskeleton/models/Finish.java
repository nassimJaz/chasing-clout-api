package com.takima.backskeleton.models;

// Rolled for every card drawn, independently of its rarity.
public enum Finish {
    NORMAL(94), HOLO(5), FIRE(1);

    public final int weight;

    Finish(int weight) {
        this.weight = weight;
    }
}
