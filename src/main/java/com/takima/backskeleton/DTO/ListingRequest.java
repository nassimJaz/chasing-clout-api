package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Finish;

public record ListingRequest(Long memeCardId, Finish finish, long price) {
}
