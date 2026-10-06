package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Finish;

public record CardPriceDto(Long memeCardId, Finish finish, long price, long buyback) {
}
