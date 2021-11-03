package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.S3MockDir;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface S3MockDirDAO extends JpaRepository<S3MockDir, Long> {

    @Query("FROM S3MockDir d WHERE d.extId = :extId")
    S3MockDir findByExtId(@Param("extId") final String extId);

}
