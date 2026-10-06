package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserDao extends JpaRepository<AppUser, Long> {
    // Atomic check + debit: returns 0 if the balance is too low, so concurrent purchases can't overspend.
    @Modifying
    @Query("UPDATE AppUser u SET u.memeBalance = u.memeBalance - :amount WHERE u.id = :id AND u.memeBalance >= :amount")
    int debit(Long id, long amount);

    @Modifying
    @Query("UPDATE AppUser u SET u.memeBalance = u.memeBalance + :amount WHERE u.id = :id")
    int credit(Long id, long amount);

    // Atomic cooldown check against the database clock (never the client's): returns 0 if the last claim is less
    // than `minutes` old. Concurrent or replayed requests wait on the row lock, then see the new claim date.
    @Modifying(flushAutomatically = true, clearAutomatically = true) // so the user is then reread with the new date
    @Query(value = """
            UPDATE app_user SET last_free_booster_claim = now()
            WHERE id = :id
              AND (last_free_booster_claim IS NULL OR last_free_booster_claim <= now() - :minutes * interval '1 minute')
            """, nativeQuery = true)
    int claimFreeBooster(Long id, int minutes);
}
