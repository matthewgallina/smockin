package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.RestfulMock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Created by mgallina.
 */
public interface RestfulMockDAO extends JpaRepository<RestfulMock, Long>, RestfulMockDAOCustom {

    RestfulMock findByExtId(final String extId);

    @Query("FROM RestfulMock m WHERE m.extId IN (:extIds) AND m.createdBy.id = :userId AND m.status = 'ACTIVE'")
    List<RestfulMock> loadAllActiveByIds(@Param("extIds") final List<String> extIds,
                                         @Param("userId") final long userId);

}
