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
    // Only change it through AppUserDao.debit, which can't go below zero.
    private long memeBalance;
    private Instant createdAt = Instant.now();

    protected AppUser() {
    }

    public AppUser(String username, long memeBalance) {
        this.username = username;
        this.memeBalance = memeBalance;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public long getMemeBalance() {
        return memeBalance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
