package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.RestfulProject;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by mgallina.
 */
public interface RestfulProjectDAO extends JpaRepository<RestfulProject, Long> {

    RestfulProject findByExtId(final String extId);
    RestfulProject findByName(final String name);

}
