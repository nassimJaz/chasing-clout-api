package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Purchase;
import com.takima.backskeleton.models.PurchaseFormat;
import com.takima.backskeleton.models.UserBooster;

// An unopened booster of the inventory. purchaseId and format are null for a booster given for free.
public record UserBoosterDto(Long id, String code, String name, Long purchaseId, PurchaseFormat format) {
    public static UserBoosterDto from(UserBooster booster) {
        Purchase purchase = booster.getPurchase();
        return new UserBoosterDto(booster.getId(), booster.getBoosterType().getCode(), booster.getBoosterType().getName(),
                purchase == null ? null : purchase.getId(), purchase == null ? null : purchase.getFormat());
    }
}
