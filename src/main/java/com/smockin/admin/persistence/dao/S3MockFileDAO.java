package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.S3MockFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface S3MockFileDAO extends JpaRepository<S3MockFile, Long> {

    @Query("FROM S3MockFile mf WHERE mf.extId = :extId")
    S3MockFile findByExtId(@Param("extId") final String extId);

}
