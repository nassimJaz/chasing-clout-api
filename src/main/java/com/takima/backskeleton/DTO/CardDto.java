package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.MemeCard;
import com.takima.backskeleton.models.Rarity;

public record CardDto(Long id, String name, String imageUrl, Rarity rarity) {
    public static CardDto from(MemeCard card) {
        return new CardDto(card.getId(), card.getName(), card.getImageUrl(), card.getRarity());
    }
}
