package com.takima.backskeleton.models;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// Read-only: the 300 cards are seeded by initdb/3_CARDS.sql.
@Entity
public class MemeCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int sourceIndex;
    private String name;
    private String imageUrl;
    private String description;
    @Enumerated(EnumType.STRING)
    private Rarity rarity;

    protected MemeCard() {
    }

    public Long getId() {
        return id;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public Rarity getRarity() {
        return rarity;
    }
}
