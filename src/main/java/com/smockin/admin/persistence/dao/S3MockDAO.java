package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.S3Mock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface S3MockDAO extends JpaRepository<S3Mock, Long> {

    @Query("FROM S3Mock m WHERE m.extId = :extId")
    S3Mock findByExtId(@Param("extId") final String extId);

    @Query("FROM S3Mock m WHERE m.bucketName = :bucketName")
    S3Mock findByBucketName(@Param("bucketName") final String bucketName);

    @Query("FROM S3Mock m WHERE m.createdBy.id = :userId")
    List<S3Mock> findAllParentsByUser(@Param("userId") final long userId);

    @Query("FROM S3Mock m WHERE m.createdBy.id = :userId AND m.status = 'ACTIVE'")
    List<S3Mock> findAllActiveParentsByUser(@Param("userId") final long userId);

}
