package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseDao extends JpaRepository<Purchase, Long> {
}
