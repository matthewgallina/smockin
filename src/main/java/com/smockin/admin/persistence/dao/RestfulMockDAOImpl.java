package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by mgallina.
 */
@Repository
public class RestfulMockDAOImpl implements RestfulMockDAOCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void detach(final RestfulMock restfulMock) {
        entityManager.detach(restfulMock);
    }

    @Override
    public List<RestfulMock> findAllByStatus(final RecordStatusEnum status) {
        return entityManager.createQuery("FROM RestfulMock rm WHERE rm.status = :status ORDER BY rm.initializationOrder ASC")
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public Map<Pair<String, RestMethodEnum>, List<RestfulMock>> findAllActivePathDuplicates() {

        final List<Object[]> groupedDuplicates = entityManager
                .createQuery("SELECT rm.path, rm.method, COUNT(rm) FROM RestfulMock rm "
                                    + " WHERE rm.status = :status "
                                    + " GROUP BY rm.path, rm.method "
                                    + " HAVING COUNT(rm) > 1")
                .setParameter("status", RecordStatusEnum.ACTIVE)
                .getResultList();

        if (groupedDuplicates.isEmpty()) {
            return null;
        }

        final List<Pair<String, RestMethodEnum>> duplicatePairs = groupedDuplicates
                .stream()
                .map(o -> Pair.of(o[0].toString(), RestMethodEnum.valueOf(o[1].toString())))
                .collect(Collectors.toList());

        final List<RestfulMock> mocks = duplicatePairs
                .stream()
                .map(r -> this.findAllActiveByPathAndMethod(r.getLeft(), r.getRight()))
                .flatMap(l -> l.stream())
                .collect(Collectors.toList());

        final Map<Pair<String, RestMethodEnum>, List<RestfulMock>> duplicatesMap = new HashMap<>();

        mocks.stream()
                .forEach(m -> {
            duplicatesMap.putIfAbsent(Pair.of(m.getPath(), m.getMethod()), new ArrayList<>());
            duplicatesMap.get(Pair.of(m.getPath(), m.getMethod())).add(m);
        });

        return duplicatesMap;
    }

    @Override
    public List<RestfulMock> findAll() {
        return entityManager.createQuery("FROM RestfulMock rm ORDER BY rm.initializationOrder ASC")
                .getResultList();
    }

    @Override
    public List<RestfulMock> findAllByUser(final long userId) {
        return entityManager.createQuery("FROM RestfulMock rm WHERE rm.createdBy.id = :userId ORDER BY rm.initializationOrder ASC")
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public RestfulMock findByPathAndMethodAndUser(final String path, final RestMethodEnum method, final SmockinUser user) {
        try {
            return entityManager.createQuery("FROM RestfulMock rm WHERE rm.path = :path AND rm.method = :method AND rm.createdBy.id = :userId", RestfulMock.class)
                    .setParameter("path", path)
                    .setParameter("method", method)
                    .setParameter("userId", user.getId())
                    .getSingleResult();
        } catch (Throwable ex) {
            return null;
        }
    }

    @Override
    public void resetAllOtherProxyPriorities(final String path, final RestMethodEnum method, final String excludeExternalId) {
        entityManager.createQuery("UPDATE RestfulMock rm SET rm.proxyPriority = false WHERE rm.path = :path AND rm.method = :method AND rm.extId != :externalId")
                .setParameter("path", path)
                .setParameter("method", method)
                .setParameter("externalId", excludeExternalId)
                .executeUpdate();
    }

    private List<RestfulMock> findAllActiveByPathAndMethod(final String path, final RestMethodEnum method) {
        return entityManager.createQuery("FROM RestfulMock rm WHERE rm.path = :path AND rm.method = :method AND rm.status = :status", RestfulMock.class)
                .setParameter("path", path)
                .setParameter("method", method)
                .setParameter("status", RecordStatusEnum.ACTIVE)
                .getResultList();
    }

}
