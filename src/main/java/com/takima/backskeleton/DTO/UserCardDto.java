package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Finish;
import com.takima.backskeleton.models.UserCard;

import java.time.Instant;

public record UserCardDto(CardDto card, Finish finish, int quantity, Instant firstObtainedAt) {
    public static UserCardDto from(UserCard userCard) {
        return new UserCardDto(CardDto.from(userCard.getMemeCard()), userCard.getFinish(), userCard.getQuantity(),
                userCard.getFirstObtainedAt());
    }
}
