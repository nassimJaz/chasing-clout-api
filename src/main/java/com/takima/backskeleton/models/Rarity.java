package com.takima.backskeleton.models;

public enum Rarity {
    COMMON(5), UNCOMMON(10), RARE(25), VERY_RARE(60), ULTRA_RARE(150), SECRET(400);

    // System price of a NORMAL card of this rarity, see Finish.priceMultiplier.
    public final long price;

    Rarity(long price) {
        this.price = price;
    }
}
