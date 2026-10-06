package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.CardListing;
import com.takima.backskeleton.models.Finish;
import com.takima.backskeleton.models.ListingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardListingDao extends JpaRepository<CardListing, Long> {
    List<CardListing> findByStatusOrderByPrice(ListingStatus status);

    // Atomic: only one buyer can take an ACTIVE listing. Two simultaneous buyers queue on the row lock and the second
    // one then sees SOLD and gets 0. Also 0 if the listing is cancelled, unknown or the buyer's own.
    @Modifying(flushAutomatically = true, clearAutomatically = true) // so findById then rereads the updated row
    @Query(value = """
            UPDATE card_listing SET status = 'SOLD', buyer_id = :buyerId, sold_at = now()
            WHERE id = :id AND status = 'ACTIVE' AND seller_id <> :buyerId
            """, nativeQuery = true)
    int markSold(Long id, Long buyerId);

    // Atomic: returns 0 if the listing isn't this seller's or isn't ACTIVE anymore (sold in the meantime).
    @Modifying(flushAutomatically = true, clearAutomatically = true) // so findById then rereads the updated row
    @Query(value = """
            UPDATE card_listing SET status = 'CANCELLED' WHERE id = :id AND seller_id = :sellerId AND status = 'ACTIVE'
            """, nativeQuery = true)
    int markCancelled(Long id, Long sellerId);

    // Prices of the latest P2P sales of one (card, finish), newest first.
    @Query("""
            SELECT l.price FROM CardListing l
            WHERE l.memeCard.id = :memeCardId AND l.finish = :finish AND l.status = :sold
            ORDER BY l.soldAt DESC
            """)
    List<Long> findRecentSalePrices(Long memeCardId, Finish finish, ListingStatus sold, Pageable page);
}