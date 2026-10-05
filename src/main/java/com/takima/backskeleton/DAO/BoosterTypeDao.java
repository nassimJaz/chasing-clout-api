package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.BoosterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoosterTypeDao extends JpaRepository<BoosterType, Long> {
}
