package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import io.javalin.http.Context;

import java.util.List;

/**
 * Created by gallina.
 */
public interface RuleEngine {

    RestfulResponseDTO process(final Context ctx, final List<RestfulMockDefinitionRule> rules);

}
