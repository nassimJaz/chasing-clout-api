package com.takima.backskeleton.models;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

// A card put on sale by a player; the card is out of the seller's collection while it is listed.
// Status changes only through CardListingDao.markSold / markCancelled, which are atomic.
@Entity
public class CardListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private AppUser seller;
    @ManyToOne
    private MemeCard memeCard;
    @Enumerated(EnumType.STRING)
    private Finish finish;
    private long price;
    @Enumerated(EnumType.STRING)
    private ListingStatus status = ListingStatus.ACTIVE;
    private Instant createdAt = Instant.now();
    @ManyToOne
    private AppUser buyer;
    private Instant soldAt;

    protected CardListing() {
    }

    public CardListing(AppUser seller, MemeCard memeCard, Finish finish, long price) {
        this.seller = seller;
        this.memeCard = memeCard;
        this.finish = finish;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public AppUser getSeller() {
        return seller;
    }

    public MemeCard getMemeCard() {
        return memeCard;
    }

    public Finish getFinish() {
        return finish;
    }

    public long getPrice() {
        return price;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AppUser getBuyer() {
        return buyer;
    }

    public Instant getSoldAt() {
        return soldAt;
    }
}