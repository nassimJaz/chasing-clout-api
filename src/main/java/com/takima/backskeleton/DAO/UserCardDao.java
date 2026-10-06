package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.UserCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCardDao extends JpaRepository<UserCard, Long> {
    List<UserCard> findByUserId(Long userId);

    // Upsert: creates the (user, card, finish) row with quantity 1, or increments it.
    @Modifying
    @Query(value = """
            INSERT INTO user_card (user_id, meme_card_id, finish, quantity) VALUES (:userId, :memeCardId, :finish, 1)
            ON CONFLICT (user_id, meme_card_id, finish) DO UPDATE SET quantity = user_card.quantity + 1
            """, nativeQuery = true)
    void addOne(Long userId, Long memeCardId, String finish);

    // Atomic: takes one copy out of the collection (decrement, or delete the last one); false if the user has none.
    default boolean removeOne(Long userId, Long memeCardId, String finish) {
        return decrementIfSeveral(userId, memeCardId, finish) == 1 || deleteIfLast(userId, memeCardId, finish) == 1;
    }

    @Modifying
    @Query(value = """
            UPDATE user_card SET quantity = quantity - 1
            WHERE user_id = :userId AND meme_card_id = :memeCardId AND finish = :finish AND quantity > 1
            """, nativeQuery = true)
    int decrementIfSeveral(Long userId, Long memeCardId, String finish);

    @Modifying
    @Query(value = """
            DELETE FROM user_card WHERE user_id = :userId AND meme_card_id = :memeCardId AND finish = :finish AND quantity = 1
            """, nativeQuery = true)
    int deleteIfLast(Long userId, Long memeCardId, String finish);
}
