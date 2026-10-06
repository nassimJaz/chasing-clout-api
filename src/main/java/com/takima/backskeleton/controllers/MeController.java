package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DAO.AppUserDao;
import com.takima.backskeleton.DAO.UserBoosterDao;
import com.takima.backskeleton.DAO.UserCardDao;
import com.takima.backskeleton.DTO.ProfileDto;
import com.takima.backskeleton.DTO.PulledCardDto;
import com.takima.backskeleton.DTO.PurchaseDto;
import com.takima.backskeleton.DTO.PurchaseRequest;
import com.takima.backskeleton.DTO.UserBoosterDto;
import com.takima.backskeleton.DTO.UserCardDto;
import com.takima.backskeleton.services.GachaService;
import com.takima.backskeleton.services.ShopService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// The current player, chosen by the X-User-Id header (demo, id 1, by default).
// ponytail: the header stands in for authentication and anyone can set it; replace it with a session or a JWT.
@CrossOrigin
@RequestMapping("me")
@RestController
public class MeController {
    static final String USER = "X-User-Id";

    private final AppUserDao appUserDao;
    private final UserBoosterDao userBoosterDao;
    private final UserCardDao userCardDao;
    private final ShopService shopService;
    private final GachaService gachaService;

    public MeController(AppUserDao appUserDao, UserBoosterDao userBoosterDao, UserCardDao userCardDao,
                        ShopService shopService, GachaService gachaService) {
        this.appUserDao = appUserDao;
        this.userBoosterDao = userBoosterDao;
        this.userCardDao = userCardDao;
        this.shopService = shopService;
        this.gachaService = gachaService;
    }

    @GetMapping("")
    public ProfileDto profile(@RequestHeader(value = USER, defaultValue = "1") Long userId) {
        return ProfileDto.from(appUserDao.findById(userId).orElseThrow());
    }

    @PostMapping("purchases")
    public PurchaseDto buy(@RequestHeader(value = USER, defaultValue = "1") Long userId,
                           @RequestBody PurchaseRequest request) {
        return PurchaseDto.from(shopService.buy(userId, request.format(), request.boosterTypeId(),
                request.secondBoosterTypeId()));
    }

    // A free Basic booster every 30 minutes (429 before that), opened with boosters/{id}/open.
    @PostMapping("free-booster")
    public UserBoosterDto claimFreeBooster(@RequestHeader(value = USER, defaultValue = "1") Long userId) {
        return UserBoosterDto.from(shopService.claimFreeBooster(userId));
    }

    // Unopened boosters.
    @GetMapping("boosters")
    public List<UserBoosterDto> boosters(@RequestHeader(value = USER, defaultValue = "1") Long userId) {
        return userBoosterDao.findByUserIdAndOpenedAtIsNull(userId).stream().map(UserBoosterDto::from).toList();
    }

    // The drawn cards, in reveal order.
    @PostMapping("boosters/{id}/open")
    public List<PulledCardDto> open(@RequestHeader(value = USER, defaultValue = "1") Long userId,
                                    @PathVariable Long id) {
        return gachaService.open(userId, id).stream().map(PulledCardDto::from).toList();
    }

    @GetMapping("cards")
    public List<UserCardDto> cards(@RequestHeader(value = USER, defaultValue = "1") Long userId) {
        return userCardDao.findByUserId(userId).stream().map(UserCardDto::from).toList();
    }
}
