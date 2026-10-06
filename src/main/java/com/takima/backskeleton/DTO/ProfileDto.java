package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.AppUser;
import com.takima.backskeleton.services.ShopService;

import java.time.Instant;

// nextFreeBoosterAt: null or in the past = the free booster can be claimed now
public record ProfileDto(Long id, String username, long memeBalance, Instant nextFreeBoosterAt) {
    public static ProfileDto from(AppUser user) {
        Instant last = user.getLastFreeBoosterClaim();
        return new ProfileDto(user.getId(), user.getUsername(), user.getMemeBalance(),
                last == null ? null : last.plus(ShopService.FREE_BOOSTER_COOLDOWN));
    }
}
