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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// The current player. No authentication yet: always the demo user.
@CrossOrigin
@RequestMapping("me")
@RestController
public class MeController {
    private static final long DEMO_USER_ID = 1L;

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
    public ProfileDto profile() {
        return ProfileDto.from(appUserDao.findById(DEMO_USER_ID).orElseThrow());
    }

    @PostMapping("purchases")
    public PurchaseDto buy(@RequestBody PurchaseRequest request) {
        return PurchaseDto.from(shopService.buy(DEMO_USER_ID, request.format(), request.boosterTypeId(),
                request.secondBoosterTypeId()));
    }

    // Unopened boosters.
    @GetMapping("boosters")
    public List<UserBoosterDto> boosters() {
        return userBoosterDao.findByUserIdAndOpenedAtIsNull(DEMO_USER_ID).stream().map(UserBoosterDto::from).toList();
    }

    // The drawn cards, in reveal order.
    @PostMapping("boosters/{id}/open")
    public List<PulledCardDto> open(@PathVariable Long id) {
        return gachaService.open(DEMO_USER_ID, id).stream().map(PulledCardDto::from).toList();
    }

    @GetMapping("cards")
    public List<UserCardDto> cards() {
        return userCardDao.findByUserId(DEMO_USER_ID).stream().map(UserCardDto::from).toList();
    }
}
