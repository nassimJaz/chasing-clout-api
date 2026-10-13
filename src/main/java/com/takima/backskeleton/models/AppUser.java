package com.takima.backskeleton.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

// Table and column names come from Spring Boot's naming strategy (AppUser -> app_user, memeBalance -> meme_balance).
@Entity
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    // BCrypt hash, set by AuthService.register
    private String passwordHash;
    // Only change it through AppUserDao.debit, which can't go below zero.
    private long memeBalance;
    private Instant createdAt = Instant.now();
    // Only set through AppUserDao.claimFreeBooster, which checks the cooldown.
    private Instant lastFreeBoosterClaim;

    protected AppUser() {
    }

    public AppUser(String username, String passwordHash, long memeBalance) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.memeBalance = memeBalance;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public long getMemeBalance() {
        return memeBalance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastFreeBoosterClaim() {
        return lastFreeBoosterClaim;
    }
}
