package com.takima.backskeleton.services;

import com.takima.backskeleton.models.Finish;
import com.takima.backskeleton.models.Rarity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Pure draw logic: no Spring, no database.
class GachaServiceTest {
    private static final int DRAWS = 1_000_000;

    @Test
    void rarityFrequenciesMatchTheWeights() {
        assertFrequencies(Map.of(Rarity.COMMON, 60, Rarity.UNCOMMON, 30, Rarity.RARE, 10));
    }

    @Test
    void equalWeightsGiveAThirdEach() {
        assertFrequencies(Map.of(Rarity.COMMON, 1, Rarity.UNCOMMON, 1, Rarity.RARE, 1));
    }

    @Test
    void finishFrequenciesMatchTheWeights() {
        assertFrequencies(Arrays.stream(Finish.values()).collect(Collectors.toMap(f -> f, f -> f.weight)));
    }

    private static <T> void assertFrequencies(Map<T, Integer> weights) {
        Random random = new Random(42);
        Map<T, Integer> counts = new HashMap<>();
        for (int i = 0; i < DRAWS; i++) {
            counts.merge(GachaService.pickWeighted(weights, random), 1, Integer::sum);
        }
        double total = weights.values().stream().mapToInt(Integer::intValue).sum();
        weights.forEach((key, weight) ->
                assertEquals(weight / total, counts.getOrDefault(key, 0) / (double) DRAWS, 0.002, key.toString()));
    }
}
