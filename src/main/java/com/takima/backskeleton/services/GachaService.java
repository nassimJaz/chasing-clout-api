package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.MemeCardDao;
import com.takima.backskeleton.DAO.PulledCardDao;
import com.takima.backskeleton.DAO.UserBoosterDao;
import com.takima.backskeleton.DAO.UserCardDao;
import com.takima.backskeleton.models.BoosterSlot;
import com.takima.backskeleton.models.Finish;
import com.takima.backskeleton.models.MemeCard;
import com.takima.backskeleton.models.PulledCard;
import com.takima.backskeleton.models.Rarity;
import com.takima.backskeleton.models.UserBooster;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

@Service
public class GachaService {
    private static final Map<Finish, Integer> FINISH_WEIGHTS = Arrays.stream(Finish.values())
            .collect(Collectors.toMap(f -> f, f -> f.weight));

    private final MemeCardDao memeCardDao;
    private final UserBoosterDao userBoosterDao;
    private final PulledCardDao pulledCardDao;
    private final UserCardDao userCardDao;
    private final RandomGenerator random;

    public GachaService(MemeCardDao memeCardDao, UserBoosterDao userBoosterDao, PulledCardDao pulledCardDao,
                        UserCardDao userCardDao, RandomGenerator random) {
        this.memeCardDao = memeCardDao;
        this.userBoosterDao = userBoosterDao;
        this.pulledCardDao = pulledCardDao;
        this.userCardDao = userCardDao;
        this.random = random;
    }

    // Opens one of the user's boosters: draws its cards, records them and adds them to the collection.
    @Transactional
    public List<PulledCard> open(Long userId, Long userBoosterId) {
        // markOpened first: it is the atomic guard against opening the same booster twice
        if (userBoosterDao.markOpened(userBoosterId, userId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booster introuvable ou déjà ouvert");
        }
        UserBooster booster = userBoosterDao.getReferenceById(userBoosterId);
        Map<Rarity, List<MemeCard>> cardsByRarity = memeCardDao.findAll().stream()
                .collect(Collectors.groupingBy(MemeCard::getRarity));

        List<PulledCard> pulled = new ArrayList<>();
        for (BoosterSlot slot : booster.getBoosterType().getSlots()) {
            for (int i = 0; i < slot.getCardCount(); i++) {
                List<MemeCard> candidates = cardsByRarity.get(pickWeighted(slot.getWeights(), random));
                MemeCard card = candidates.get(random.nextInt(candidates.size()));
                Finish finish = pickWeighted(FINISH_WEIGHTS, random);
                pulled.add(new PulledCard(booster, card, finish, pulled.size() + 1));
                userCardDao.addOne(userId, card.getId(), finish.name());
            }
        }
        return pulledCardDao.saveAll(pulled);
    }

    // Picks a key with probability weight / sum of weights.
    static <T> T pickWeighted(Map<T, Integer> weights, RandomGenerator random) {
        int r = random.nextInt(weights.values().stream().mapToInt(Integer::intValue).sum());
        for (Map.Entry<T, Integer> e : weights.entrySet()) {
            r -= e.getValue();
            if (r < 0) {
                return e.getKey();
            }
        }
        throw new IllegalArgumentException("weights must be positive");
    }
}
