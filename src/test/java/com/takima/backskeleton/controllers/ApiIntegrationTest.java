package com.takima.backskeleton.controllers;

import com.jayway.jsonpath.JsonPath;
import com.takima.backskeleton.DAO.AppUserDao;
import com.takima.backskeleton.DAO.UserCardDao;
import com.takima.backskeleton.models.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Needs the database (docker compose up -d); rolled back at the end. Plays with fresh players, logged in with as(id)
// (a JWT built by spring-security-test, without going through /auth: AuthApiIntegrationTest covers that).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private AppUserDao appUserDao;
    @Autowired
    private UserCardDao userCardDao;

    @Test
    void buyOpenAndCollect() throws Exception {
        String player = player(5000);
        mvc.perform(get("/cards")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(300)));

        String boosters = mvc.perform(get("/boosters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].code").value("BASIC"))
                .andExpect(jsonPath("$[0].prices.TRI_PACK").value(240))
                .andExpect(jsonPath("$[2].slots", hasSize(3)))
                .andReturn().getResponse().getContentAsString();
        int basicId = JsonPath.read(boosters, "$[0].id");

        mvc.perform(post("/me/purchases").with(as(player)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"format\": \"SINGLE\", \"boosterTypeId\": " + basicId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(100));

        String inventory = mvc.perform(get("/me/boosters").with(as(player)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("BASIC"))
                .andReturn().getResponse().getContentAsString();
        int boosterId = JsonPath.read(inventory, "$[0].id");

        mvc.perform(post("/me/boosters/" + boosterId + "/open").with(as(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].card.imageUrl").exists())
                .andExpect(jsonPath("$[0].finish").exists());
        mvc.perform(post("/me/boosters/" + boosterId + "/open").with(as(player)))
                .andExpect(status().isConflict());

        mvc.perform(get("/me/boosters").with(as(player))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/me/cards").with(as(player))).andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void notEnoughMemesIsAConflict() throws Exception {
        String boosters = mvc.perform(get("/boosters")).andReturn().getResponse().getContentAsString();
        int collectionId = JsonPath.read(boosters, "$[2].id");

        mvc.perform(post("/me/purchases").with(as(player(5000))).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"format\": \"DISPLAY_MEGA\", \"boosterTypeId\": " + collectionId + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void marketplaceAndFreeBooster() throws Exception {
        String seller = player(0);
        String buyer = player(1000);
        userCardDao.addOne(Long.valueOf(seller), 1L, "NORMAL");

        mvc.perform(get("/market/prices/1")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));

        String listing = mvc.perform(post("/market/listings").with(as(seller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memeCardId\": 1, \"finish\": \"NORMAL\", \"price\": 300}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        int listingId = JsonPath.read(listing, "$.id");

        mvc.perform(delete("/market/listings/" + listingId).with(as(buyer)))
                .andExpect(status().isConflict()); // not the buyer's listing
        mvc.perform(post("/market/listings/" + listingId + "/purchase").with(as(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"));

        mvc.perform(post("/me/free-booster").with(as(buyer))).andExpect(status().isOk());
        mvc.perform(post("/me/free-booster").with(as(buyer))).andExpect(status().isTooManyRequests());
        mvc.perform(get("/me").with(as(buyer))).andExpect(jsonPath("$.nextFreeBoosterAt").exists());
    }

    // A request made by this player: a token whose subject is their id, like AuthService's.
    private static RequestPostProcessor as(String player) {
        return jwt().jwt(token -> token.subject(player));
    }

    private String player(long balance) {
        return appUserDao.save(new AppUser("api-" + System.nanoTime(), "x", balance)).getId().toString();
    }
}
