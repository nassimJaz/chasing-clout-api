package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.AppUserDao;
import com.takima.backskeleton.DAO.BoosterTypeDao;
import com.takima.backskeleton.DAO.PurchaseDao;
import com.takima.backskeleton.DAO.UserBoosterDao;
import com.takima.backskeleton.models.AppUser;
import com.takima.backskeleton.models.BoosterType;
import com.takima.backskeleton.models.Purchase;
import com.takima.backskeleton.models.PurchaseFormat;
import com.takima.backskeleton.models.UserBooster;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class ShopService {
    public static final Duration FREE_BOOSTER_COOLDOWN = Duration.ofMinutes(30);

    private final AppUserDao appUserDao;
    private final BoosterTypeDao boosterTypeDao;
    private final PurchaseDao purchaseDao;
    private final UserBoosterDao userBoosterDao;

    public ShopService(AppUserDao appUserDao, BoosterTypeDao boosterTypeDao, PurchaseDao purchaseDao,
                       UserBoosterDao userBoosterDao) {
        this.appUserDao = appUserDao;
        this.boosterTypeDao = boosterTypeDao;
        this.purchaseDao = purchaseDao;
        this.userBoosterDao = userBoosterDao;
    }

    // Buys a format of boosterTypeId; with secondBoosterTypeId (Display Medium/Mega only) it is half of each.
    // The boosters land unopened in the inventory.
    @Transactional
    public Purchase buy(Long userId, PurchaseFormat format, Long boosterTypeId, Long secondBoosterTypeId) {
        int count = format.boosterCount;
        BoosterType first = findType(boosterTypeId);
        List<BoosterType> boosters = new ArrayList<>();
        long total;
        if (secondBoosterTypeId == null) {
            total = price(first.getPrice(), count);
            for (int i = 0; i < count; i++) {
                boosters.add(first);
            }
        } else {
            if (format != PurchaseFormat.DISPLAY_MEDIUM && format != PurchaseFormat.DISPLAY_MEGA) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mixte 50/50 n'existe qu'en Display Medium et Méga");
            }
            BoosterType second = findType(secondBoosterTypeId);
            total = price(first.getPrice(), count / 2) + price(second.getPrice(), count / 2);
            for (int i = 0; i < count / 2; i++) {
                boosters.add(first);
                boosters.add(second);
            }
        }

        // debit first: it is the atomic guard against spending more than the balance
        if (appUserDao.debit(userId, total) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solde insuffisant");
        }
        AppUser user = appUserDao.getReferenceById(userId);
        Purchase purchase = purchaseDao.save(new Purchase(user, format, total));
        userBoosterDao.saveAll(boosters.stream().map(type -> new UserBooster(user, type, purchase)).toList());
        return purchase;
    }

    // A free Basic booster (unopened), at most one per FREE_BOOSTER_COOLDOWN. The check uses the database clock and
    // the row lock (AppUserDao.claimFreeBooster), so neither the client's clock nor replayed requests matter.
    @Transactional
    public UserBooster claimFreeBooster(Long userId) {
        if (appUserDao.claimFreeBooster(userId, (int) FREE_BOOSTER_COOLDOWN.toMinutes()) != 1) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Booster gratuit déjà réclamé, réessaie plus tard");
        }
        BoosterType basic = boosterTypeDao.findByCode("BASIC").orElseThrow();
        return userBoosterDao.save(new UserBooster(appUserDao.getReferenceById(userId), basic, null));
    }

    // count boosters at unitPrice, count / 3 of them at -60%.
    public static long price(long unitPrice, int count) {
        int discounted = count / 3;
        return (count - discounted) * unitPrice + discounted * unitPrice * 40 / 100;
    }

    private BoosterType findType(Long id) {
        return boosterTypeDao.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booster inconnu"));
    }
}
