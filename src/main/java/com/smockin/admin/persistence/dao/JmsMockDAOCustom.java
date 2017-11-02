package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.JmsMock;
import com.smockin.admin.persistence.enums.RecordStatusEnum;

import java.util.List;

/**
 * Created by mgallina.
 */
public interface JmsMockDAOCustom {

    void detach(final JmsMock jmsQueueMock);
    List<JmsMock> findAllByStatus(final RecordStatusEnum status);
    List<JmsMock> findAll();

}
