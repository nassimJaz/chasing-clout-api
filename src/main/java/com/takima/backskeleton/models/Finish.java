package com.takima.backskeleton.models;

// Rolled for every card drawn, independently of its rarity. It is the card's "effect" on the market.
public enum Finish {
    NORMAL(94, 1), HOLO(5, 4), FIRE(1, 15);

    public final int weight;
    // System price = Rarity.price x priceMultiplier (e.g. Rare Holo = 25 x 4 = 100 memes).
    public final long priceMultiplier;

    Finish(int weight, long priceMultiplier) {
        this.weight = weight;
        this.priceMultiplier = priceMultiplier;
    }
}
