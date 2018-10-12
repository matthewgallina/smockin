package com.smockin.mockserver.engine;

import com.smockin.admin.enums.DeploymentStatusEnum;
import com.smockin.admin.persistence.entity.Identifier;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import java.util.Date;
import java.util.Map;

/**
 * Created by mgallina.
 */
public interface MockServerEngine<C extends MockedServerConfigDTO, D> extends BaseServerEngine<C, D> {

    void start(final C config, D data) throws MockServerException;
    Map<Long, Date> loadDeployedMocks();

    default DeploymentStatusEnum getDeploymentStatus(final Identifier entityIdentifier) {

        // TODO fix this!

        if (!loadDeployedMocks().containsKey(entityIdentifier.getId())) {
            return DeploymentStatusEnum.INACTIVE;
        }

        final Date lastUpdated = loadDeployedMocks().get(entityIdentifier.getId());

        if (lastUpdated == null && entityIdentifier.getLastUpdated() == null) {
            return DeploymentStatusEnum.ACTIVE;
        }

        if (lastUpdated != null && entityIdentifier.getLastUpdated() != null) {

            if (lastUpdated.equals(entityIdentifier.getLastUpdated())) {
                return DeploymentStatusEnum.ACTIVE;
            }

            return DeploymentStatusEnum.PENDING;
        }

        return DeploymentStatusEnum.INACTIVE;
    }

}
