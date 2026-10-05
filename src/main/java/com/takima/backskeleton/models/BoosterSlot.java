package com.takima.backskeleton.models;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;

import java.util.Map;

// A slot draws cardCount cards; each card's rarity is picked with the relative weights.
@Entity
public class BoosterSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int position;
    private int cardCount;
    @ElementCollection
    @CollectionTable(name = "booster_slot_odds", joinColumns = @JoinColumn(name = "booster_slot_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "rarity")
    @Column(name = "weight")
    private Map<Rarity, Integer> weights;

    protected BoosterSlot() {
    }

    public Long getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public int getCardCount() {
        return cardCount;
    }

    public Map<Rarity, Integer> getWeights() {
        return weights;
    }
}
