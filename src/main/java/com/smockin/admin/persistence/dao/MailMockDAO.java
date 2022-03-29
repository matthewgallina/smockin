package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.MailMock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MailMockDAO extends JpaRepository<MailMock, Long> {

    @Query("FROM MailMock m WHERE m.extId = :extId")
    MailMock findByExtId(@Param("extId") final String extId);

    @Query("FROM MailMock m WHERE m.extId = :extId AND m.createdBy.id = :userId")
    MailMock findByExtIdAndUser(@Param("extId") final String extId, @Param("userId") final long userId);

    @Query("FROM MailMock m WHERE m.createdBy.id = :userId")
    List<MailMock> findAllByUser(@Param("userId") final long userId);

    @Query("FROM MailMock m WHERE m.status = 'ACTIVE'")
    List<MailMock> findAllActive();

}
