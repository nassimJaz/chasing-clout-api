package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.BoosterType;
import com.takima.backskeleton.models.PurchaseFormat;
import com.takima.backskeleton.models.Rarity;
import com.takima.backskeleton.services.ShopService;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// prices: total price of each format. A 50/50 mixed display costs (price of A + price of B) / 2 for that format.
public record BoosterTypeDto(Long id, String code, String name, long price, Map<PurchaseFormat, Long> prices,
                             List<Slot> slots) {
    public record Slot(int position, int cardCount, Map<Rarity, Integer> weights) {
    }

    public static BoosterTypeDto from(BoosterType type) {
        Map<PurchaseFormat, Long> prices = new EnumMap<>(PurchaseFormat.class);
        for (PurchaseFormat format : PurchaseFormat.values()) {
            prices.put(format, ShopService.price(type.getPrice(), format.boosterCount));
        }
        List<Slot> slots = type.getSlots().stream()
                .map(s -> new Slot(s.getPosition(), s.getCardCount(), new EnumMap<>(s.getWeights())))
                .toList();
        return new BoosterTypeDto(type.getId(), type.getCode(), type.getName(), type.getPrice(), prices, slots);
    }
}
