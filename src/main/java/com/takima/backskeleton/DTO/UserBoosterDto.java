package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.UserBooster;

// An unopened booster of the inventory.
public record UserBoosterDto(Long id, String code, String name) {
    public static UserBoosterDto from(UserBooster booster) {
        return new UserBoosterDto(booster.getId(), booster.getBoosterType().getCode(), booster.getBoosterType().getName());
    }
}
