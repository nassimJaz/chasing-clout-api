package com.takima.backskeleton.services;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pure price indexing: no Spring, no database.
class MarketServiceTest {

    @Test
    void notRegularlySoldKeepsTheAlgorithmicPrice() {
        assertEquals(20, MarketService.basePrice(20, List.of(500L, 500L)));
    }

    @Test
    void higherSalesRaiseThePriceWithSmoothing() {
        // 20 + 30% x (95% x 200 - 20) = 20 + 51
        assertEquals(71, MarketService.basePrice(20, List.of(200L, 200L, 200L)));
    }

    @Test
    void priceAlwaysStaysBelowTheSalesAverage() {
        for (long algo : List.of(5L, 100L, 999L)) {
            for (long sale : List.of(1L, 100L, 1000L, 50_000L)) {
                long price = MarketService.basePrice(algo, List.of(sale, sale, sale));
                assertTrue(price < sale || price == algo, algo + " / " + sale + " -> " + price);
            }
        }
    }

    @Test
    void fallingMarketNeverGoesUnderTheAlgorithmicPrice() {
        assertEquals(400, MarketService.basePrice(400, List.of(10L, 10L, 10L)));
    }
}
