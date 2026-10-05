package com.takima.backskeleton.models;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

// One card drawn from an opened booster (history of the draws, in reveal order).
@Entity
public class PulledCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private UserBooster userBooster;
    @ManyToOne
    private MemeCard memeCard;
    @Enumerated(EnumType.STRING)
    private Finish finish;
    private int position;

    protected PulledCard() {
    }

    public PulledCard(UserBooster userBooster, MemeCard memeCard, Finish finish, int position) {
        this.userBooster = userBooster;
        this.memeCard = memeCard;
        this.finish = finish;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public UserBooster getUserBooster() {
        return userBooster;
    }

    public MemeCard getMemeCard() {
        return memeCard;
    }

    public Finish getFinish() {
        return finish;
    }

    public int getPosition() {
        return position;
    }
}
