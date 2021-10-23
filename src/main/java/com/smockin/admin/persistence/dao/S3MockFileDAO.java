package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.S3MockFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface S3MockFileDAO extends JpaRepository<S3MockFile, Long> {

}
