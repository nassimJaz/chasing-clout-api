package com.takima.backskeleton.services;

import com.takima.backskeleton.models.Finish;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Real concurrent requests, so each call commits in its own transaction (no @Transactional here).
// Needs the database; the players created here and everything they own are deleted after each test.
@SpringBootTest
class MarketConcurrencyTest {
    @Autowired
    private MarketService marketService;
    @Autowired
    private ShopService shopService;
    @Autowired
    private JdbcTemplate jdbc;

    private final List<Long> users = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long id : users) {
            jdbc.update("DELETE FROM user_booster WHERE user_id = ?", id);
            jdbc.update("DELETE FROM card_listing WHERE seller_id = ? OR buyer_id = ?", id, id);
            jdbc.update("DELETE FROM user_card WHERE user_id = ?", id);
        }
        users.forEach(id -> jdbc.update("DELETE FROM app_user WHERE id = ?", id));
    }

    @Test
    void twoBuyersAtTheSameTimeOnlyOneGetsTheCard() throws Exception {
        Long seller = user(0);
        Long buyer1 = user(1000);
        Long buyer2 = user(1000);
        Long listing = listOneCard(seller, 300);

        int sold = successes(List.of(() -> marketService.buy(buyer1, listing), () -> marketService.buy(buyer2, listing)));

        assertEquals(1, sold);
        assertEquals(300, balance(seller));
        assertEquals(1700, balance(buyer1) + balance(buyer2));
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM user_card WHERE user_id IN (?, ?)", Integer.class, buyer1, buyer2));
    }

    @Test
    void failedPaymentPutsTheListingBackOnSale() {
        Long seller = user(0);
        Long poorBuyer = user(100);
        Long listing = listOneCard(seller, 300);

        assertThrows(ResponseStatusException.class, () -> marketService.buy(poorBuyer, listing));

        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM card_listing WHERE id = ?", String.class, listing));
        assertEquals(0, balance(seller));
        assertEquals(100, balance(poorBuyer));
    }

    @Test
    void simultaneousFreeBoosterClaimsGiveOnlyOne() throws Exception {
        Long player = user(0);

        int claimed = successes(Collections.nCopies(5, () -> shopService.claimFreeBooster(player)));

        assertEquals(1, claimed);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_booster WHERE user_id = ?", Integer.class, player));
    }

    // Starts all the tasks at the same instant; returns how many succeeded (the others got a 4xx).
    private int successes(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (Callable<Object> task : tasks) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    task.call();
                    return true;
                } catch (ResponseStatusException e) {
                    return false;
                }
            }));
        }
        start.countDown();
        int ok = 0;
        for (Future<Boolean> result : results) {
            ok += result.get() ? 1 : 0;
        }
        pool.shutdown();
        return ok;
    }

    private Long user(long balance) {
        Long id = jdbc.queryForObject("INSERT INTO app_user (username, password_hash, meme_balance) VALUES (?, 'x', ?) RETURNING id",
                Long.class, "test-" + System.nanoTime(), balance);
        users.add(id);
        return id;
    }

    private Long listOneCard(Long seller, long price) {
        Long card = jdbc.queryForObject("SELECT min(id) FROM meme_card", Long.class);
        jdbc.update("INSERT INTO user_card (user_id, meme_card_id, finish, quantity) VALUES (?, ?, 'NORMAL', 1)", seller, card);
        return marketService.list(seller, card, Finish.NORMAL, price).getId();
    }

    private long balance(Long userId) {
        return jdbc.queryForObject("SELECT meme_balance FROM app_user WHERE id = ?", Long.class, userId);
    }
}
