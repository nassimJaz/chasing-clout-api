package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.AppUserDao;
import com.takima.backskeleton.DAO.CardListingDao;
import com.takima.backskeleton.DAO.MemeCardDao;
import com.takima.backskeleton.DAO.UserCardDao;
import com.takima.backskeleton.models.AppUser;
import com.takima.backskeleton.models.CardListing;
import com.takima.backskeleton.models.Finish;
import com.takima.backskeleton.models.ListingStatus;
import com.takima.backskeleton.models.MemeCard;
import com.takima.backskeleton.models.Rarity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Needs the database (docker compose up -d); every test is rolled back. Players are created by the tests.
@SpringBootTest
@Transactional
class MarketServiceIntegrationTest {
    @Autowired
    private MarketService marketService;
    @Autowired
    private ShopService shopService;
    @Autowired
    private AppUserDao appUserDao;
    @Autowired
    private MemeCardDao memeCardDao;
    @Autowired
    private UserCardDao userCardDao;
    @Autowired
    private CardListingDao cardListingDao;
    @Autowired
    private EntityManager entityManager;

    private MemeCard common;
    private Long seller;
    private Long buyer;

    @BeforeEach
    void setUp() {
        common = memeCardDao.findAll().stream().filter(c -> c.getRarity() == Rarity.COMMON).findFirst().orElseThrow();
        seller = appUserDao.save(new AppUser("seller-" + System.nanoTime(), "x", 1000)).getId();
        buyer = appUserDao.save(new AppUser("buyer-" + System.nanoTime(), "x", 1000)).getId();
    }

    @Test
    void listingMovesTheCardAndTheMoney() {
        userCardDao.addOne(seller, common.getId(), "NORMAL");
        CardListing listing = marketService.list(seller, common.getId(), Finish.NORMAL, 300);
        assertEquals(0, quantity(seller)); // in escrow while on sale

        marketService.buy(buyer, listing.getId());

        entityManager.clear(); // balances are changed by bulk updates
        assertEquals(1300, balance(seller));
        assertEquals(700, balance(buyer));
        assertEquals(1, quantity(buyer));
        assertEquals(ListingStatus.SOLD, cardListingDao.findById(listing.getId()).orElseThrow().getStatus());
        assertThrows(ResponseStatusException.class, () -> marketService.buy(buyer, listing.getId()));
    }

    @Test
    void cannotListACardYouDontOwnNorBuyYourOwnListing() {
        assertThrows(ResponseStatusException.class, () -> marketService.list(seller, common.getId(), Finish.HOLO, 10));

        userCardDao.addOne(seller, common.getId(), "NORMAL");
        CardListing listing = marketService.list(seller, common.getId(), Finish.NORMAL, 300);
        assertThrows(ResponseStatusException.class, () -> marketService.buy(seller, listing.getId()));
    }

    @Test
    void cancellingGivesTheCardBack() {
        userCardDao.addOne(seller, common.getId(), "NORMAL");
        CardListing listing = marketService.list(seller, common.getId(), Finish.NORMAL, 300);

        marketService.cancel(seller, listing.getId());

        entityManager.clear();
        assertEquals(1, quantity(seller));
        assertThrows(ResponseStatusException.class, () -> marketService.buy(buyer, listing.getId()));
    }

    @Test
    void basePriceFollowsTheP2PSales() {
        // Common Holo: 5 x 4 = 20 memes until it is regularly sold
        assertEquals(20, marketService.basePrice(common, Finish.HOLO));

        for (int i = 0; i < MarketService.MIN_SALES; i++) {
            userCardDao.addOne(seller, common.getId(), "HOLO");
            marketService.buy(buyer, marketService.list(seller, common.getId(), Finish.HOLO, 200).getId());
        }

        // 20 + 30% x (95% x 200 - 20): raised, but under the 200 the players pay
        assertEquals(71, marketService.basePrice(common, Finish.HOLO));
        assertEquals(35, marketService.buyback(common, Finish.HOLO));
    }

    @Test
    void systemBuysBackAtHalfThePrice() {
        userCardDao.addOne(seller, common.getId(), "NORMAL");
        long buyback = marketService.buyback(common, Finish.NORMAL);

        assertEquals(buyback, marketService.sellToSystem(seller, common.getId(), Finish.NORMAL));

        entityManager.clear();
        assertEquals(1000 + buyback, balance(seller));
        assertEquals(0, quantity(seller));
        // no copy left: refused and nothing credited
        assertThrows(ResponseStatusException.class,
                () -> marketService.sellToSystem(seller, common.getId(), Finish.NORMAL));
        entityManager.clear();
        assertEquals(1000 + buyback, balance(seller));
    }

    @Test
    void freeBoosterEveryThirtyMinutes() {
        shopService.claimFreeBooster(buyer);
        assertThrows(ResponseStatusException.class, () -> shopService.claimFreeBooster(buyer));

        entityManager.createNativeQuery(
                        "UPDATE app_user SET last_free_booster_claim = now() - interval '31 minutes' WHERE id = ?1")
                .setParameter(1, buyer)
                .executeUpdate();
        shopService.claimFreeBooster(buyer);
    }

    private long balance(Long userId) {
        return appUserDao.findById(userId).orElseThrow().getMemeBalance();
    }

    private int quantity(Long userId) {
        return userCardDao.findByUserId(userId).stream()
                .filter(c -> c.getMemeCard().getId().equals(common.getId()) && c.getFinish() == Finish.NORMAL)
                .mapToInt(c -> c.getQuantity())
                .sum();
    }
}
