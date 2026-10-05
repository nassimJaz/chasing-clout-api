package com.takima.backskeleton.models;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

// One shop order; the boosters it bought are the UserBooster rows pointing to it.
@Entity
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private AppUser user;
    @Enumerated(EnumType.STRING)
    private PurchaseFormat format;
    private long totalPrice;
    private Instant createdAt = Instant.now();

    protected Purchase() {
    }

    public Purchase(AppUser user, PurchaseFormat format, long totalPrice) {
        this.user = user;
        this.format = format;
        this.totalPrice = totalPrice;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public PurchaseFormat getFormat() {
        return format;
    }

    public long getTotalPrice() {
        return totalPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
