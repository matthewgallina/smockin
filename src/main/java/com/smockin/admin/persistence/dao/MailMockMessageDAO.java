package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.MailMockMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MailMockMessageDAO extends JpaRepository<MailMockMessage, Long> {

    @Query("FROM MailMockMessage m WHERE m.extId = :extId AND m.mailMock.id = :mailMockId")
    MailMockMessage findByExtId(@Param("mailMockId") final long mailMockId,
                                @Param("extId") final String extId);

    @Query("FROM MailMockMessage m WHERE m.extId = :extId")
    MailMockMessage findByExtId(@Param("extId") final String extId);

    @Query("SELECT COUNT(1) FROM MailMockMessageAttachment m WHERE m.mailMockMessage.id = :id")
    Integer findAttachmentCountByMessageId(@Param("id") final long messageId);

    @Query("FROM MailMockMessage m WHERE m.mailMock.id = :mailMockId ORDER BY m.dateReceived DESC")
    Page<MailMockMessage> findAllMessageByMailMockId(@Param("mailMockId") final long mailMockId,
                                                     final Pageable pageable);

    @Query("SELECT COUNT(1) FROM MailMockMessage m WHERE m.mailMock.id = :mailMockId")
    Integer countAllMessageByMailMockId(@Param("mailMockId") final long mailMockId);

}
