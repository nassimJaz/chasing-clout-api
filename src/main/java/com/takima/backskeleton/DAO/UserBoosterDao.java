package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.UserBooster;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBoosterDao extends JpaRepository<UserBooster, Long> {
    // Fetch-joined: no extra select per booster type / purchase.
    @EntityGraph(attributePaths = {"boosterType", "purchase"})
    List<UserBooster> findByUserIdAndOpenedAtIsNull(Long userId);

    // Atomic: returns 0 if the booster isn't this user's or is already opened (no double opening).
    @Modifying
    @Query("UPDATE UserBooster b SET b.openedAt = INSTANT WHERE b.id = :id AND b.user.id = :userId AND b.openedAt IS NULL")
    int markOpened(Long id, Long userId);
}
