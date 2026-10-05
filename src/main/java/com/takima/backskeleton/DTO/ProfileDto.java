package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.AppUser;

public record ProfileDto(Long id, String username, long memeBalance) {
    public static ProfileDto from(AppUser user) {
        return new ProfileDto(user.getId(), user.getUsername(), user.getMemeBalance());
    }
}
