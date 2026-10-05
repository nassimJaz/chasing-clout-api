package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.AppUserDao;
import com.takima.backskeleton.DAO.BoosterTypeDao;
import com.takima.backskeleton.DAO.UserBoosterDao;
import com.takima.backskeleton.DAO.UserCardDao;
import com.takima.backskeleton.models.AppUser;
import com.takima.backskeleton.models.BoosterType;
import com.takima.backskeleton.models.PulledCard;
import com.takima.backskeleton.models.UserBooster;
import com.takima.backskeleton.models.UserCard;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Needs the database (docker compose up -d); every test is rolled back.
@SpringBootTest
@Transactional
class GachaServiceIntegrationTest {
    @Autowired
    private GachaService gachaService;
    @Autowired
    private AppUserDao appUserDao;
    @Autowired
    private BoosterTypeDao boosterTypeDao;
    @Autowired
    private UserBoosterDao userBoosterDao;
    @Autowired
    private UserCardDao userCardDao;
    @Autowired
    private EntityManager entityManager;

    @ParameterizedTest
    @CsvSource({"BASIC, 10", "SPECIAL, 10", "COLLECTION, 12"})
    void openDrawsTheRightCardsOnce(String code, int expectedCards) {
        AppUser demo = appUserDao.findById(1L).orElseThrow();
        BoosterType type = boosterTypeDao.findAll().stream().filter(t -> t.getCode().equals(code)).findFirst().orElseThrow();
        UserBooster booster = userBoosterDao.save(new UserBooster(demo, type, null));
        int cardsBefore = collectionSize(demo);

        List<PulledCard> pulled = gachaService.open(demo.getId(), booster.getId());

        assertEquals(expectedCards, pulled.size());
        for (int i = 0; i < pulled.size(); i++) {
            assertEquals(i + 1, pulled.get(i).getPosition());
        }
        // each card's rarity is one the slot it comes from can give
        int position = 0;
        for (var slot : type.getSlots()) {
            for (int i = 0; i < slot.getCardCount(); i++) {
                assertTrue(slot.getWeights().containsKey(pulled.get(position++).getMemeCard().getRarity()));
            }
        }
        entityManager.clear(); // addOne is a native upsert, reload the collection from the database
        assertEquals(cardsBefore + expectedCards, collectionSize(demo));
        assertThrows(ResponseStatusException.class, () -> gachaService.open(demo.getId(), booster.getId()));
    }

    private int collectionSize(AppUser user) {
        return userCardDao.findByUserId(user.getId()).stream().mapToInt(UserCard::getQuantity).sum();
    }
}
