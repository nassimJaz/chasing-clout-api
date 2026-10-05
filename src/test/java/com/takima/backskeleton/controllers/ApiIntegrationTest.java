package com.takima.backskeleton.controllers;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Needs the database (docker compose up -d); rolled back at the end.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void buyOpenAndCollect() throws Exception {
        mvc.perform(get("/cards")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(300)));

        String boosters = mvc.perform(get("/boosters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].code").value("BASIC"))
                .andExpect(jsonPath("$[0].prices.TRI_PACK").value(240))
                .andExpect(jsonPath("$[2].slots", hasSize(3)))
                .andReturn().getResponse().getContentAsString();
        int basicId = JsonPath.read(boosters, "$[0].id");

        mvc.perform(post("/me/purchases").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"format\": \"SINGLE\", \"boosterTypeId\": " + basicId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(100));

        String inventory = mvc.perform(get("/me/boosters"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("BASIC"))
                .andReturn().getResponse().getContentAsString();
        int boosterId = JsonPath.read(inventory, "$[0].id");

        mvc.perform(post("/me/boosters/" + boosterId + "/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].card.imageUrl").exists())
                .andExpect(jsonPath("$[0].finish").exists());
        mvc.perform(post("/me/boosters/" + boosterId + "/open")).andExpect(status().isConflict());

        mvc.perform(get("/me/boosters")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/me/cards")).andExpect(jsonPath("$", not(empty())));
        mvc.perform(get("/me")).andExpect(jsonPath("$.username").value("demo"));
    }

    @Test
    void notEnoughMemesIsAConflict() throws Exception {
        String boosters = mvc.perform(get("/boosters")).andReturn().getResponse().getContentAsString();
        int collectionId = JsonPath.read(boosters, "$[2].id");

        mvc.perform(post("/me/purchases").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"format\": \"DISPLAY_MEGA\", \"boosterTypeId\": " + collectionId + "}"))
                .andExpect(status().isConflict());
    }
}
