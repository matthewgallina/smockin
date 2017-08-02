package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by mgallina.
 */
public interface RestfulMockDefinitionRuleDAO extends JpaRepository<RestfulMockDefinitionRule, Long> {

    RestfulMockDefinitionRule findByExtId(final String extId);

}
