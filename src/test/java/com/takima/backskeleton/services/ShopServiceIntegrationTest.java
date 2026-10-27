package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.AppUserDao;
import com.takima.backskeleton.DAO.BoosterTypeDao;
import com.takima.backskeleton.DAO.UserBoosterDao;
import com.takima.backskeleton.models.AppUser;
import com.takima.backskeleton.models.BoosterSlot;
import com.takima.backskeleton.models.BoosterType;
import com.takima.backskeleton.models.Finish;
import com.takima.backskeleton.models.Purchase;
import com.takima.backskeleton.models.PurchaseFormat;
import com.takima.backskeleton.models.UserBooster;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Needs the database (docker compose up -d); every test is rolled back. Each test buys with a fresh 5000-meme player.
@SpringBootTest
@Transactional
class ShopServiceIntegrationTest {
    private Long player;

    @Autowired
    private ShopService shopService;
    @Autowired
    private AppUserDao appUserDao;
    @Autowired
    private BoosterTypeDao boosterTypeDao;
    @Autowired
    private UserBoosterDao userBoosterDao;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void createPlayer() {
        player = appUserDao.save(new AppUser("shop-" + System.nanoTime(), "x", 5000)).getId();
    }

    @Test
    void buyDebitsAndAddsUnopenedBoosters() {
        Purchase purchase = shopService.buy(player, PurchaseFormat.TRI_PACK, type("SPECIAL"), null);

        assertEquals(600, purchase.getTotalPrice());
        assertEquals(4400, balance());
        List<UserBooster> boosters = boostersOf(purchase);
        assertEquals(3, boosters.size());
        boosters.forEach(b -> assertEquals("SPECIAL", b.getBoosterType().getCode()));
    }

    @Test
    void mixedDisplayIsHalfOfEach() {
        Purchase purchase = shopService.buy(player, PurchaseFormat.DISPLAY_MEDIUM, type("BASIC"), type("SPECIAL"));

        assertEquals(4200, purchase.getTotalPrice()); // (10 x 100 + 5 x 40) + (10 x 250 + 5 x 100)
        assertEquals(800, balance());
        List<UserBooster> boosters = boostersOf(purchase);
        assertEquals(15, boosters.stream().filter(b -> b.getBoosterType().getCode().equals("BASIC")).count());
        assertEquals(15, boosters.stream().filter(b -> b.getBoosterType().getCode().equals("SPECIAL")).count());
    }

    @Test
    void notEnoughMemesBuysNothing() {
        assertThrows(ResponseStatusException.class,
                () -> shopService.buy(player, PurchaseFormat.DISPLAY_MEGA, type("COLLECTION"), null));

        assertEquals(5000, balance());
        assertEquals(0, userBoosterDao.findByUserIdAndOpenedAtIsNull(player).size());
    }

    @Test
    void mixIsOnlyForMediumAndMega() {
        assertThrows(ResponseStatusException.class,
                () -> shopService.buy(player, PurchaseFormat.TRI_PACK, type("BASIC"), type("SPECIAL")));

        assertEquals(5000, balance());
    }

    private Long type(String code) {
        return boosterTypeDao.findAll().stream().filter(t -> t.getCode().equals(code)).findFirst().map(BoosterType::getId).orElseThrow();
    }

    private long balance() {
        entityManager.clear(); // debit is a bulk update, reload the user from the database
        return appUserDao.findById(player).orElseThrow().getMemeBalance();
    }

    // Economy guard, read-only: what a booster is expected to resell for (odds from the database x average finish
    // multiplier x buyback) must stay under its price in a display, where a third is at -60%.
    @Test
    void resellingABoosterToTheSystemLosesMemes() {
        double finishMultiplier = Arrays.stream(Finish.values()).mapToDouble(f -> f.weight * f.priceMultiplier).sum()
                / Arrays.stream(Finish.values()).mapToInt(f -> f.weight).sum(); // 1.29
        for (BoosterType type : boosterTypeDao.findAll()) {
            double cardsValue = 0;
            for (BoosterSlot slot : type.getSlots()) {
                double total = slot.getWeights().values().stream().mapToInt(Integer::intValue).sum();
                double meanRarityPrice = slot.getWeights().entrySet().stream()
                        .mapToDouble(e -> e.getKey().price * e.getValue() / total).sum();
                cardsValue += slot.getCardCount() * meanRarityPrice;
            }
            double resale = cardsValue * finishMultiplier * MarketService.BUYBACK_RATE;
            double displayUnitPrice = ShopService.price(type.getPrice(), 30) / 30.0;
            assertTrue(resale < displayUnitPrice, type.getCode() + ": resale " + resale + " >= " + displayUnitPrice);
        }
    }

    private List<UserBooster> boostersOf(Purchase purchase) {
        return userBoosterDao.findByUserIdAndOpenedAtIsNull(player).stream()
                .filter(b -> b.getPurchase().getId().equals(purchase.getId())).toList();
    }
}
