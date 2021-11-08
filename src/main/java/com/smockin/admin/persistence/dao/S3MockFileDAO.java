package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.S3MockFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface S3MockFileDAO extends JpaRepository<S3MockFile, Long> {

    @Query("FROM S3MockFile mf WHERE mf.extId = :extId")
    S3MockFile findByExtId(@Param("extId") final String extId);

    @Query("FROM S3MockFile mf WHERE mf.name = :name")
    List<S3MockFile> findAllByName(@Param("name") final String name);

    /*

        with RECURSIVE T(id, S3_MOCK_DIR_PARENT, name) as (
           select id, S3_MOCK_DIR_PARENT, name
           from S3_MOCK_DIR AS d
        where name = 'www'
        union all
           select dd.id, dd.S3_MOCK_DIR_PARENT, dd.name
           from T inner join S3_MOCK_DIR AS dd on dd.id = t.S3_MOCK_DIR_PARENT
        )
        select *
        from T;

    */

}
