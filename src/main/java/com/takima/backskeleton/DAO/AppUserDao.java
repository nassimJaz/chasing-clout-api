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
}
