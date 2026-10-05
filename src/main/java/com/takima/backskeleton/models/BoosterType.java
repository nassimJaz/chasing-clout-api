package com.takima.backskeleton.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import java.util.List;

// Read-only: seeded by initdb/2_DEFAULT_ENTRIES.sql (BASIC, SPECIAL, COLLECTION).
@Entity
public class BoosterType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;
    private long price;
    @OneToMany
    @JoinColumn(name = "booster_type_id")
    @OrderBy("position")
    private List<BoosterSlot> slots;

    protected BoosterType() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public long getPrice() {
        return price;
    }

    public List<BoosterSlot> getSlots() {
        return slots;
    }
}
