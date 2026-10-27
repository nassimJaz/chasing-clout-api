package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.AppUserDao;
import com.takima.backskeleton.DAO.CardListingDao;
import com.takima.backskeleton.DAO.MemeCardDao;
import com.takima.backskeleton.DAO.UserCardDao;
import com.takima.backskeleton.models.CardListing;
import com.takima.backskeleton.models.Finish;
import com.takima.backskeleton.models.ListingStatus;
import com.takima.backskeleton.models.MemeCard;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MarketService {
    // "Regularly sold": the price only moves after this many P2P sales.
    static final int MIN_SALES = 3;
    // ponytail: "recent" = the last 10 sales, whatever their age; add a time window if old sales skew prices.
    static final int RECENT_SALES = 10;
    // The system buys cards back at 30% of their base price, and never sells any: low enough that opening boosters
    // and reselling everything loses memes (see ShopServiceIntegrationTest.resellingABoosterToTheSystemLosesMemes).
    static final double BUYBACK_RATE = 0.3;

    private final AppUserDao appUserDao;
    private final MemeCardDao memeCardDao;
    private final UserCardDao userCardDao;
    private final CardListingDao cardListingDao;

    public MarketService(AppUserDao appUserDao, MemeCardDao memeCardDao, UserCardDao userCardDao,
                         CardListingDao cardListingDao) {
        this.appUserDao = appUserDao;
        this.memeCardDao = memeCardDao;
        this.userCardDao = userCardDao;
        this.cardListingDao = cardListingDao;
    }

    // Computed on read (indexed query on at most 10 rows), so it is always up to date and needs no worker.
    public long basePrice(MemeCard card, Finish finish) {
        List<Long> recent = cardListingDao.findRecentSalePrices(card.getId(), finish, ListingStatus.SOLD,
                PageRequest.of(0, RECENT_SALES));
        return basePrice(card.getRarity().price * finish.priceMultiplier, recent);
    }

    // Algorithmic price, raised by 30% (smoothing) of its gap to 95% of the recent sales average:
    // it moves towards the market but always stays below that average. A falling market never goes under algoPrice.
    static long basePrice(long algoPrice, List<Long> recentPrices) {
        if (recentPrices.size() < MIN_SALES) {
            return algoPrice;
        }
        double target = 0.95 * recentPrices.stream().mapToLong(Long::longValue).average().orElseThrow();
        return algoPrice + Math.max(0, (long) (0.30 * (target - algoPrice)));
    }

    public long buyback(MemeCard card, Finish finish) {
        return Math.max(1, (long) (basePrice(card, finish) * BUYBACK_RATE));
    }

    // Sells one copy to the system at the buyback price; returns the amount credited.
    @Transactional
    public long sellToSystem(Long userId, Long memeCardId, Finish finish) {
        long amount = buyback(findCard(memeCardId, finish), finish);
        if (!userCardDao.removeOne(userId, memeCardId, finish.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tu ne possèdes pas cette carte");
        }
        appUserDao.credit(userId, amount);
        return amount;
    }

    // Puts one copy on sale: it leaves the seller's collection until it is sold or the listing cancelled.
    @Transactional
    public CardListing list(Long sellerId, Long memeCardId, Finish finish, long price) {
        MemeCard card = findCard(memeCardId, finish);
        if (price <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le prix doit être positif");
        }
        if (!userCardDao.removeOne(sellerId, memeCardId, finish.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tu ne possèdes pas cette carte");
        }
        return cardListingDao.save(new CardListing(appUserDao.getReferenceById(sellerId), card, finish, price));
    }

    @Transactional
    public void cancel(Long sellerId, Long listingId) {
        if (cardListingDao.markCancelled(listingId, sellerId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Annonce introuvable, déjà vendue ou pas à toi");
        }
        CardListing listing = cardListingDao.findById(listingId).orElseThrow();
        userCardDao.addOne(sellerId, listing.getMemeCard().getId(), listing.getFinish().name());
    }

    // One transaction: claim the listing, move the money, give the card. Any failure rolls everything back,
    // including the claim, so the listing is on sale again.
    @Transactional
    public CardListing buy(Long buyerId, Long listingId) {
        // markSold first: it is the atomic guard against two buyers taking the same card
        if (cardListingDao.markSold(listingId, buyerId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Annonce déjà vendue, retirée ou à toi");
        }
        CardListing listing = cardListingDao.findById(listingId).orElseThrow();
        Long sellerId = listing.getSeller().getId();
        // balances are locked in id order, so crossed purchases (A buys from B while B buys from A) can't deadlock
        if (buyerId < sellerId) {
            debit(buyerId, listing.getPrice());
        }
        appUserDao.credit(sellerId, listing.getPrice());
        if (buyerId > sellerId) {
            debit(buyerId, listing.getPrice());
        }
        userCardDao.addOne(buyerId, listing.getMemeCard().getId(), listing.getFinish().name());
        return listing;
    }

    private void debit(Long userId, long amount) {
        if (appUserDao.debit(userId, amount) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solde insuffisant");
        }
    }

    private MemeCard findCard(Long memeCardId, Finish finish) {
        if (memeCardId == null || finish == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memeCardId et finish sont obligatoires");
        }
        return memeCardDao.findById(memeCardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carte inconnue"));
    }
}
