package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.PulledCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PulledCardDao extends JpaRepository<PulledCard, Long> {
}
