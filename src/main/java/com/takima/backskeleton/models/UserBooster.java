package com.takima.backskeleton.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

// A booster in the user's inventory; openedAt stays null until it is opened.
@Entity
public class UserBooster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private AppUser user;
    @ManyToOne
    private BoosterType boosterType;
    // null for a booster given for free
    @ManyToOne
    private Purchase purchase;
    // Only set through UserBoosterDao.markOpened, so a booster can't be opened twice.
    private Instant openedAt;

    protected UserBooster() {
    }

    public UserBooster(AppUser user, BoosterType boosterType, Purchase purchase) {
        this.user = user;
        this.boosterType = boosterType;
        this.purchase = purchase;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public BoosterType getBoosterType() {
        return boosterType;
    }

    public Purchase getPurchase() {
        return purchase;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }
}
