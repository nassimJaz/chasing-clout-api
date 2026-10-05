package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Purchase;
import com.takima.backskeleton.models.PurchaseFormat;

import java.time.Instant;

public record PurchaseDto(Long id, PurchaseFormat format, long totalPrice, Instant createdAt) {
    public static PurchaseDto from(Purchase purchase) {
        return new PurchaseDto(purchase.getId(), purchase.getFormat(), purchase.getTotalPrice(), purchase.getCreatedAt());
    }
}
