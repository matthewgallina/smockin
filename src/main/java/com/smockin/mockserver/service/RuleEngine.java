package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import com.smockin.mockserver.service.dto.RestfulResponse;
import spark.Request;

import java.util.List;

/**
 * Created by gallina.
 */
public interface RuleEngine {

    RestfulResponse process(final Request req, final List<RestfulMockDefinitionRule> rules);

}
