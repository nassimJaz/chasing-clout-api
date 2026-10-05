package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.PurchaseFormat;

// secondBoosterTypeId: null, or the other half of a 50/50 Display Medium/Mega
public record PurchaseRequest(PurchaseFormat format, Long boosterTypeId, Long secondBoosterTypeId) {
}
