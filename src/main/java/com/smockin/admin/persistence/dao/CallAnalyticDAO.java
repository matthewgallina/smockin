package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.CallAnalytic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CallAnalyticDAO extends JpaRepository<CallAnalytic, Long> {

    @Query("FROM CallAnalytic ca WHERE ca.extId = :extId")
    CallAnalytic findByExtId(@Param("extId") final String extId);

    @Query("FROM CallAnalytic ca WHERE ca.createdBy.id = :userId")
    List<CallAnalytic> findAll(@Param("userId") final long userId);

    @Query("FROM CallAnalytic ca WHERE ca.status = 'ACTIVE'")
    List<CallAnalytic> findAllActive();

}
