package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.MQMock;
import com.smockin.admin.persistence.enums.MQTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MQMockDAO extends JpaRepository<MQMock, Long> {

    @Query("FROM MQMock m WHERE m.extId = :extId")
    MQMock findByExtId(@Param("extId") final String extId);

    @Query("FROM MQMock m WHERE m.name = :name")
    MQMock findByName(@Param("name") final String name);

    @Query("FROM MQMock m WHERE m.extId = :extId AND m.createdBy.id = :userId")
    MQMock findByExtIdAndUser(@Param("extId") final String extId, @Param("userId") final long userId);

    @Query("FROM MQMock m WHERE m.createdBy.id = :userId")
    List<MQMock> findAllByUser(@Param("userId") final long userId);

    @Query("FROM MQMock m WHERE m.status = 'ACTIVE'")
    List<MQMock> findAllActive();

    @Query("FROM MQMock m WHERE m.mqType = :mqType AND m.status = 'ACTIVE'")
    List<MQMock> findAllActiveByType(@Param("mqType") final MQTypeEnum mqType);

    @Query("FROM MQMock m WHERE m.destinationName = :destinationName AND m.mqType = :mqType AND m.status = 'ACTIVE'")
    MQMock findByDestinationAndType(@Param("destinationName") final String destinationName,
                                    @Param("mqType") final MQTypeEnum mqType);
}
