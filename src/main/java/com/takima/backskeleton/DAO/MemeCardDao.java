package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.MemeCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemeCardDao extends JpaRepository<MemeCard, Long> {
}
