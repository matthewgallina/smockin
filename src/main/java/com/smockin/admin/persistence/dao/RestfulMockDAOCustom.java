package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

/**
 * Created by mgallina.
 */
public interface RestfulMockDAOCustom {

    void detach(final RestfulMock restfulMock);
    List<RestfulMock> findAllByStatus(final RecordStatusEnum status);
    Map<Pair<String, RestMethodEnum>, List<RestfulMock>> findAllActivePathDuplicates();
    List<RestfulMock> findAll();
    List<RestfulMock> findAllByUser(final long userId);
    RestfulMock findByPathAndMethodAndUser(final String path, final RestMethodEnum method, final SmockinUser user);
    void resetAllOtherProxyPriorities(final String path, final RestMethodEnum method, final String excludeExternalId);

}
