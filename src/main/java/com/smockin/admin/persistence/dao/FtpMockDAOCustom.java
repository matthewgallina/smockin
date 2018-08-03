package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.FtpMock;
import com.smockin.admin.persistence.enums.RecordStatusEnum;

import java.util.List;

public interface FtpMockDAOCustom {

    void detach(final FtpMock ftpMock);
    List<FtpMock> findAllByStatus(final RecordStatusEnum status);
    List<FtpMock> findAllByUser(final long userId);

}
