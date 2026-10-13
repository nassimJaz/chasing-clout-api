package com.takima.backskeleton;

import com.takima.backskeleton.DAO.AppUserDao;
import com.takima.backskeleton.DAO.BoosterTypeDao;
import com.takima.backskeleton.DAO.MemeCardDao;
import com.takima.backskeleton.DAO.UserBoosterDao;
import com.takima.backskeleton.DAO.UserCardDao;
import com.takima.backskeleton.models.CardListing;
import com.takima.backskeleton.models.Finish;
import com.takima.backskeleton.models.PurchaseFormat;
import com.takima.backskeleton.models.Rarity;
import com.takima.backskeleton.models.UserCard;
import com.takima.backskeleton.services.AuthService;
import com.takima.backskeleton.services.GachaService;
import com.takima.backskeleton.services.MarketService;
import com.takima.backskeleton.services.ShopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

// Demo players for whoever tests the app: only with the "demo" profile (docker-compose.demo.yml), never on the dev base.
// Made through the real services, so the data follows the game rules. Runs at startup, once: skipped if prof exists.
@Component
@Profile("demo")
public class DemoData implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoData.class);
    // rival's listings, as a % of the system price: some bargains, some overpriced
    private static final int[] PRICE_PERCENTS = {200, 150, 120, 100, 80, 60};

    private final AuthService authService;
    private final ShopService shopService;
    private final GachaService gachaService;
    private final MarketService marketService;
    private final AppUserDao appUserDao;
    private final BoosterTypeDao boosterTypeDao;
    private final UserBoosterDao userBoosterDao;
    private final UserCardDao userCardDao;
    private final MemeCardDao memeCardDao;

    public DemoData(AuthService authService, ShopService shopService, GachaService gachaService,
                    MarketService marketService, AppUserDao appUserDao, BoosterTypeDao boosterTypeDao,
                    UserBoosterDao userBoosterDao, UserCardDao userCardDao, MemeCardDao memeCardDao) {
        this.authService = authService;
        this.shopService = shopService;
        this.gachaService = gachaService;
        this.marketService = marketService;
        this.appUserDao = appUserDao;
        this.boosterTypeDao = boosterTypeDao;
        this.userBoosterDao = userBoosterDao;
        this.userCardDao = userCardDao;
        this.memeCardDao = memeCardDao;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (appUserDao.findByUsername("prof").isPresent()) {
            log.info("Données de démo déjà présentes");
            return;
        }
        Long rival = player("rival", "rival1234");
        Long prof = player("prof", "prof1234");
        appUserDao.credit(prof, 10_000); // enough to try the big displays

        // rival opens a Special mini display and puts their 6 rarest cards on the market
        buyAndOpen(rival, PurchaseFormat.DISPLAY_MINI, "SPECIAL");
        List<UserCard> rarest = userCardDao.findByUserId(rival).stream()
                .sorted(Comparator.comparing((UserCard c) -> c.getMemeCard().getRarity()).reversed())
                .limit(PRICE_PERCENTS.length)
                .toList();
        CardListing cheapest = null;
        for (int i = 0; i < rarest.size(); i++) {
            UserCard c = rarest.get(i);
            long price = Math.max(1, marketService.basePrice(c.getMemeCard(), c.getFinish()) * PRICE_PERCENTS[i] / 100);
            CardListing listing = marketService.list(rival, c.getMemeCard().getId(), c.getFinish(), price);
            if (cheapest == null || price < cheapest.getPrice()) {
                cheapest = listing;
            }
        }

        // prof: a collection (opened boosters, plus a holo and a fire card), a single and a tri-pack still to open
        buyAndOpen(prof, PurchaseFormat.SINGLE, "COLLECTION");
        buyAndOpen(prof, PurchaseFormat.TRI_PACK, "BASIC");
        userCardDao.addOne(prof, anyCard(Rarity.ULTRA_RARE), Finish.HOLO.name());
        userCardDao.addOne(prof, anyCard(Rarity.SECRET), Finish.FIRE.name());
        shopService.buy(prof, PurchaseFormat.SINGLE, typeId("SPECIAL"), null);
        shopService.buy(prof, PurchaseFormat.TRI_PACK, typeId("COLLECTION"), null);

        // a sale between them (price history) and a listing of prof's own (to try "Retirer")
        marketService.buy(prof, cheapest.getId());
        UserCard mine = userCardDao.findByUserId(prof).stream()
                .filter(c -> c.getFinish() == Finish.NORMAL).findFirst().orElseThrow();
        marketService.list(prof, mine.getMemeCard().getId(), Finish.NORMAL,
                marketService.basePrice(mine.getMemeCard(), Finish.NORMAL) * 2);

        log.info("Données de démo prêtes : prof / prof1234 et rival / rival1234");
    }

    private Long player(String username, String password) {
        authService.register(username, password);
        return appUserDao.findByUsername(username).orElseThrow().getId();
    }

    // Opens every unopened booster of the player: call it before buying the boosters meant to stay unopened.
    private void buyAndOpen(Long userId, PurchaseFormat format, String boosterCode) {
        shopService.buy(userId, format, typeId(boosterCode), null);
        userBoosterDao.findByUserIdAndOpenedAtIsNull(userId).forEach(b -> gachaService.open(userId, b.getId()));
    }

    private Long typeId(String code) {
        return boosterTypeDao.findByCode(code).orElseThrow().getId();
    }

    private Long anyCard(Rarity rarity) {
        return memeCardDao.findAll().stream().filter(c -> c.getRarity() == rarity).findFirst().orElseThrow().getId();
    }
}
