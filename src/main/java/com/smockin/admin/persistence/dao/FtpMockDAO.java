package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.FtpMock;
import com.smockin.admin.persistence.entity.JmsMock;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by mgallina.
 */
public interface FtpMockDAO extends JpaRepository<FtpMock, Long>, FtpMockDAOCustom {

    FtpMock findByExtId(final String extId);

}
