package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.MQMockMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MQMockMessageDAO extends JpaRepository<MQMockMessage, Long> {

    @Query("FROM MQMockMessage m WHERE m.extId = :extId")
    MQMockMessage findByExtId(@Param("extId") final String extId);

    @Query("FROM MQMockMessage m WHERE m.mqMock.extId = :mqMockExtId ORDER BY m.dateReceived DESC")
    List<MQMockMessage> findByMqMockExtId(@Param("mqMockExtId") final String mqMockExtId);

    @Query("FROM MQMockMessage m WHERE m.mqMock.extId = :mqMockExtId ORDER BY m.dateReceived DESC")
    org.springframework.data.domain.Page<MQMockMessage> findByMqMockExtIdPaged(
            @Param("mqMockExtId") final String mqMockExtId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(m) FROM MQMockMessage m WHERE m.mqMock.extId = :mqMockExtId")
    long countByMqMockExtId(@Param("mqMockExtId") final String mqMockExtId);

    @Query("FROM MQMockMessage m WHERE m.mqMock.id = :mqMockId ORDER BY m.dateReceived DESC")
    List<MQMockMessage> findByMqMockId(@Param("mqMockId") final long mqMockId);

    void deleteByMqMockExtId(@Param("mqMockExtId") final String mqMockExtId);
}
