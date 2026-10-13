package com.takima.backskeleton;

import com.takima.backskeleton.DAO.AppUserDao;
import com.takima.backskeleton.DAO.BoosterTypeDao;
import com.takima.backskeleton.DAO.CardListingDao;
import com.takima.backskeleton.DAO.MemeCardDao;
import com.takima.backskeleton.DAO.UserBoosterDao;
import com.takima.backskeleton.DAO.UserCardDao;
import com.takima.backskeleton.models.Finish;
import com.takima.backskeleton.models.ListingStatus;
import com.takima.backskeleton.models.UserCard;
import com.takima.backskeleton.services.AuthService;
import com.takima.backskeleton.services.GachaService;
import com.takima.backskeleton.services.MarketService;
import com.takima.backskeleton.services.ShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// The demo data the teacher logs into (docker-compose.demo.yml). Needs the database; rolled back at the end, so it
// never lands in the dev base (the "demo" profile isn't active in tests, DemoData is built by hand).
@SpringBootTest
@Transactional
class DemoDataTest {
    @Autowired
    private AuthService authService;
    @Autowired
    private ShopService shopService;
    @Autowired
    private GachaService gachaService;
    @Autowired
    private MarketService marketService;
    @Autowired
    private AppUserDao appUserDao;
    @Autowired
    private BoosterTypeDao boosterTypeDao;
    @Autowired
    private UserBoosterDao userBoosterDao;
    @Autowired
    private UserCardDao userCardDao;
    @Autowired
    private MemeCardDao memeCardDao;
    @Autowired
    private CardListingDao cardListingDao;

    @Test
    void createsTheDemoPlayersOnce() {
        assumeTrue(appUserDao.findByUsername("prof").isEmpty(), "a prof account already exists in this database");
        DemoData demoData = new DemoData(authService, shopService, gachaService, marketService, appUserDao,
                boosterTypeDao, userBoosterDao, userCardDao, memeCardDao);

        demoData.run(null);
        demoData.run(null); // second start: skipped, no "pseudo déjà pris"

        Long prof = appUserDao.findByUsername("prof").orElseThrow().getId();
        Long rival = appUserDao.findByUsername("rival").orElseThrow().getId();
        authService.login("prof", "prof1234"); // the password in the README works (throws a 401 otherwise)
        assertEquals(4, userBoosterDao.findByUserIdAndOpenedAtIsNull(prof).size()); // a single + a tri-pack
        List<UserCard> cards = userCardDao.findByUserId(prof);
        assertTrue(cards.stream().anyMatch(c -> c.getFinish() == Finish.HOLO));
        assertTrue(cards.stream().anyMatch(c -> c.getFinish() == Finish.FIRE));

        var listings = cardListingDao.findByStatusOrderByPrice(ListingStatus.ACTIVE);
        assertEquals(5, listings.stream().filter(l -> l.getSeller().getId().equals(rival)).count()); // 6 listed, 1 sold
        assertEquals(1, listings.stream().filter(l -> l.getSeller().getId().equals(prof)).count());
    }
}
