package com.takima.backskeleton.models;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

// The user's collection: one row per (card, finish). Rows are created/incremented by UserCardDao.addOne.
@Entity
public class UserCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private AppUser user;
    @ManyToOne
    private MemeCard memeCard;
    @Enumerated(EnumType.STRING)
    private Finish finish;
    private int quantity;
    private Instant firstObtainedAt;

    protected UserCard() {
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public MemeCard getMemeCard() {
        return memeCard;
    }

    public Finish getFinish() {
        return finish;
    }

    public int getQuantity() {
        return quantity;
    }

    public Instant getFirstObtainedAt() {
        return firstObtainedAt;
    }
}
