package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DAO.BoosterTypeDao;
import com.takima.backskeleton.DAO.MemeCardDao;
import com.takima.backskeleton.DTO.BoosterTypeDto;
import com.takima.backskeleton.DTO.CardDto;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// What exists in the game, the same for every player.
@CrossOrigin
@RestController
public class CatalogController {
    private final MemeCardDao memeCardDao;
    private final BoosterTypeDao boosterTypeDao;

    public CatalogController(MemeCardDao memeCardDao, BoosterTypeDao boosterTypeDao) {
        this.memeCardDao = memeCardDao;
        this.boosterTypeDao = boosterTypeDao;
    }

    @GetMapping("cards")
    public List<CardDto> cards() {
        return memeCardDao.findAll(Sort.by("id")).stream().map(CardDto::from).toList();
    }

    @GetMapping("boosters")
    public List<BoosterTypeDto> boosters() {
        return boosterTypeDao.findAll(Sort.by("price")).stream().map(BoosterTypeDto::from).toList();
    }
}
