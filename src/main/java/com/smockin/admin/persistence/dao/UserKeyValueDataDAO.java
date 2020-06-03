package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.UserKeyValueData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserKeyValueDataDAO extends JpaRepository<UserKeyValueData, Long> {

    @Query("FROM UserKeyValueData kvp WHERE kvp.createdBy.id = :userId")
    List<UserKeyValueData> findAllByUser(@Param("userId") final long userId);

    UserKeyValueData findByExtId(final String extId);

    @Query("FROM UserKeyValueData kvp WHERE LOWER(kvp.key) = LOWER(:key) AND kvp.createdBy.id = :userId")
    UserKeyValueData findByKey(@Param("key") final String key, @Param("userId") final long userId);

}
