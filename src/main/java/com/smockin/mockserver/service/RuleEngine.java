package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import spark.Request;

import java.util.List;

/**
 * Created by gallina.
 */
public interface RuleEngine {

    RestfulResponseDTO process(final Request req, final List<RestfulMockDefinitionRule> rules);

}
