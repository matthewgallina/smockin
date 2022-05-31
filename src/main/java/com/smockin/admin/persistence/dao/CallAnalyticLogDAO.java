package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.CallAnalyticLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CallAnalyticLogDAO extends JpaRepository<CallAnalyticLog, Long> {

    @Query("SELECT COUNT(1) FROM CallAnalyticLog cal WHERE cal.callAnalytic.id = :id AND cal.originType = 'RESTFUL'")
    Integer countAllApiLogsForCallAnalyticId(@Param("id") final long callAnalyticId);

}
