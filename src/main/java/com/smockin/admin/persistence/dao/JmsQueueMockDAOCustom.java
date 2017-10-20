package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.JmsQueueMock;
import com.smockin.admin.persistence.enums.RecordStatusEnum;

import java.util.List;

/**
 * Created by mgallina.
 */
public interface JmsQueueMockDAOCustom {

    void detach(final JmsQueueMock jmsQueueMock);
    List<JmsQueueMock> findAllByStatus(final RecordStatusEnum status);
    List<JmsQueueMock> findAll();

}
