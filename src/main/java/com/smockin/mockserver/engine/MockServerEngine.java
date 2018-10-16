package com.smockin.mockserver.engine;

import com.smockin.admin.enums.DeploymentStatusEnum;
import com.smockin.admin.persistence.entity.Identifier;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Created by mgallina.
 */
public interface MockServerEngine<C extends MockedServerConfigDTO, D> extends BaseServerEngine<C, D> {

    void start(final C config, D data) throws MockServerException;
    Map<Long, Date> loadDeployedMocks();

    default DeploymentStatusEnum getDeploymentStatus(final Identifier entityIdentifier, final RecordStatusEnum status) {

        // Mock Server is down so all mocks are in effect un-deployed so return OFFLINE
        if (!getCurrentState().isRunning()) {
            return DeploymentStatusEnum.OFFLINE;
        }

        final boolean isPresent = loadDeployedMocks().containsKey(entityIdentifier.getId());

        // Mock Server is up...

        if (!isPresent) {

            // mock is not currently deployed...
            switch (status) {
                case ACTIVE:
                    return DeploymentStatusEnum.PENDING;
                case INACTIVE:
                    return DeploymentStatusEnum.OFFLINE;
                default:
                    throw new MockServerException("Unable to determine deployment status of mock");
            }
        }

        final Optional<Date> lastUpdatedOpt = Optional.ofNullable(loadDeployedMocks().get(entityIdentifier.getId()));
        final Optional<Date> latestLastUpdatedOpt = Optional.ofNullable(entityIdentifier.getLastUpdated());

        // mock is deployed and has been updated, so display as PENDING
        if (!lastUpdatedOpt.equals(latestLastUpdatedOpt)) {
            return DeploymentStatusEnum.PENDING;
        }

        // mock is deployed and has not been updated, so display as DEPLOYED
        return DeploymentStatusEnum.DEPLOYED;
    }

}
