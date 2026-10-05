package com.takima.backskeleton.services;

import com.takima.backskeleton.models.PurchaseFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Pure price logic: no Spring, no database.
class ShopServiceTest {

    @ParameterizedTest
    @CsvSource({
            "SINGLE, 100, 100",          // 1 full price
            "TRI_PACK, 100, 240",        // 2 x 100 + 1 x 40
            "DISPLAY_MINI, 100, 720",    // 6 x 100 + 3 x 40
            "DISPLAY_MEDIUM, 100, 2400", // 20 x 100 + 10 x 40
            "DISPLAY_MEGA, 100, 4800",   // 40 x 100 + 20 x 40
            "TRI_PACK, 250, 600",        // 2 x 250 + 1 x 100
            "SINGLE, 500, 500",
    })
    void price(PurchaseFormat format, long unitPrice, long expected) {
        assertEquals(expected, ShopService.price(unitPrice, format.boosterCount));
    }

    @Test
    void mixedDisplaysDiscountAThirdOfEachHalf() {
        // Medium Basic + Collection: (10 x 100 + 5 x 40) + (10 x 500 + 5 x 200)
        assertEquals(7200, ShopService.price(100, 15) + ShopService.price(500, 15));
        // Mega Special + Collection: (20 x 250 + 10 x 100) + (20 x 500 + 10 x 200)
        assertEquals(18000, ShopService.price(250, 30) + ShopService.price(500, 30));
    }
}
