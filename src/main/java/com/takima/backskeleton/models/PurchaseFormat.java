package com.takima.backskeleton.models;

public enum PurchaseFormat {
    SINGLE(1), TRI_PACK(3), DISPLAY_MINI(9), DISPLAY_MEDIUM(30), DISPLAY_MEGA(60);

    public final int boosterCount;

    PurchaseFormat(int boosterCount) {
        this.boosterCount = boosterCount;
    }
}
