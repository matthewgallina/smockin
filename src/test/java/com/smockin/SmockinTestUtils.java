package com.smockin;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;

/**
 * Created by mgallina on 30/08/17.
 */
public final class SmockinTestUtils {

    public static RestfulMock buildRestfulMock(final String path, final RestMockTypeEnum mockType, final int initOrder, final RestMethodEnum restMethod, final RecordStatusEnum status) {

        final RestfulMock mock = new RestfulMock();
        mock.setPath(path);
        mock.setMockType(mockType);
        mock.setInitializationOrder(initOrder);
        mock.setMethod(restMethod);
        mock.setStatus(status);

        return mock;
    }

}
