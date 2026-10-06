package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.CardListing;
import com.takima.backskeleton.models.Finish;
import com.takima.backskeleton.models.ListingStatus;

import java.time.Instant;

public record ListingDto(Long id, CardDto card, Finish finish, long price, ListingStatus status, String seller,
                         Instant createdAt) {
    public static ListingDto from(CardListing listing) {
        return new ListingDto(listing.getId(), CardDto.from(listing.getMemeCard()), listing.getFinish(),
                listing.getPrice(), listing.getStatus(), listing.getSeller().getUsername(), listing.getCreatedAt());
    }
}
