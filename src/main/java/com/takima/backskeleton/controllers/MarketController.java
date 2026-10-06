package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DAO.CardListingDao;
import com.takima.backskeleton.DAO.MemeCardDao;
import com.takima.backskeleton.DTO.CardPriceDto;
import com.takima.backskeleton.DTO.CardRequest;
import com.takima.backskeleton.DTO.ListingDto;
import com.takima.backskeleton.DTO.ListingRequest;
import com.takima.backskeleton.models.Finish;
import com.takima.backskeleton.models.ListingStatus;
import com.takima.backskeleton.models.MemeCard;
import com.takima.backskeleton.services.MarketService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

// System buyback of single cards and P2P listings. The current player comes from the X-User-Id header (see MeController).
@CrossOrigin
@RequestMapping("market")
@RestController
public class MarketController {
    private final MarketService marketService;
    private final MemeCardDao memeCardDao;
    private final CardListingDao cardListingDao;

    public MarketController(MarketService marketService, MemeCardDao memeCardDao, CardListingDao cardListingDao) {
        this.marketService = marketService;
        this.memeCardDao = memeCardDao;
        this.cardListingDao = cardListingDao;
    }

    // System price of the card in each finish, and what the system pays to buy it back.
    @GetMapping("prices/{memeCardId}")
    public List<CardPriceDto> prices(@PathVariable Long memeCardId) {
        MemeCard card = memeCardDao.findById(memeCardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carte inconnue"));
        return Arrays.stream(Finish.values())
                .map(f -> new CardPriceDto(memeCardId, f, marketService.basePrice(card, f),
                        marketService.buyback(card, f)))
                .toList();
    }

    // Sells one copy to the system; returns the amount credited.
    @PostMapping("system-sales")
    public long sellToSystem(@RequestHeader(value = MeController.USER, defaultValue = "1") Long userId,
                             @RequestBody CardRequest request) {
        return marketService.sellToSystem(userId, request.memeCardId(), request.finish());
    }

    // Cards on sale, cheapest first.
    @GetMapping("listings")
    public List<ListingDto> listings() {
        return cardListingDao.findByStatusOrderByPrice(ListingStatus.ACTIVE).stream().map(ListingDto::from).toList();
    }

    @PostMapping("listings")
    public ListingDto list(@RequestHeader(value = MeController.USER, defaultValue = "1") Long userId,
                           @RequestBody ListingRequest request) {
        return ListingDto.from(marketService.list(userId, request.memeCardId(), request.finish(), request.price()));
    }

    // Cancels the listing and gives the card back.
    @DeleteMapping("listings/{id}")
    public void cancel(@RequestHeader(value = MeController.USER, defaultValue = "1") Long userId,
                       @PathVariable Long id) {
        marketService.cancel(userId, id);
    }

    @PostMapping("listings/{id}/purchase")
    public ListingDto buy(@RequestHeader(value = MeController.USER, defaultValue = "1") Long userId,
                          @PathVariable Long id) {
        return ListingDto.from(marketService.buy(userId, id));
    }
}
