package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.AntPathMatcher;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by mgallina.
 */
@Repository
public class RestfulMockDAOImpl implements RestfulMockDAOCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<RestfulMock> findAllByStatus(final RecordStatusEnum status) {
        return entityManager.createQuery("FROM RestfulMock rm "
                + " WHERE rm.status = :status "
                + " ORDER BY rm.initializationOrder ASC")
                .setParameter("status", status)
                .getResultList();
    }

    /*
    @Override
    public List<RestfulMock> findAllByStatusAndUser(final RecordStatusEnum status, final long userId) {
        return entityManager.createQuery("FROM RestfulMock rm "
                + " WHERE rm.createdBy.id = :userId "
                + " AND rm.status = :status "
                + " ORDER BY rm.initializationOrder ASC")
                .setParameter("userId", userId)
                .setParameter("status", status)
                .getResultList();
    }
    */

    @Override
    public List<RestfulMock> findAll() {
        return entityManager.createQuery("FROM RestfulMock rm "
                + " ORDER BY rm.initializationOrder ASC")
                .getResultList();
    }

    @Override
    public List<RestfulMock> findAllByUser(final long userId) {
        return entityManager.createQuery("FROM RestfulMock rm "
                + " WHERE rm.createdBy.id = :userId "
                + " ORDER BY rm.initializationOrder ASC")
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public RestfulMock findByPathAndMethodAndUser(final String path, final RestMethodEnum method, final SmockinUser user) {
        try {
            return entityManager.createQuery("FROM RestfulMock rm "
                    + " WHERE rm.path = :path "
                    + " AND rm.method = :method "
                    + " AND rm.createdBy.id = :userId", RestfulMock.class)
                    .setParameter("path", path)
                    .setParameter("method", method)
                    .setParameter("userId", user.getId())
                    .getSingleResult();
        } catch (Throwable ex) {
            return null;
        }
    }

    /*
    @Override
    public RestfulMock findActiveByMethodAndPathPattern(final RestMethodEnum method, final String path) {

        final String part1 = StringUtils.split(path, AntPathMatcher.DEFAULT_PATH_SEPARATOR)[0];

        final List<RestfulMock> mocks = entityManager.createQuery("FROM RestfulMock rm "
                + " WHERE rm.method = :method "
                + " AND (rm.path = :path1 OR rm.path LIKE '/'||:path2||'%')"
                + " AND rm.status = 'ACTIVE'", RestfulMock.class)
                .setParameter("method", method)
                .setParameter("path1", path)
                .setParameter("path2", part1)
                .getResultList();

        return matchPath(mocks, path);
    }
    */

    @Override
    public RestfulMock findActiveByMethodAndPathPatternAndTypesForSingleUser(final RestMethodEnum method, final String path, final List<RestMockTypeEnum> mockTypes) {

        final String part1 = StringUtils.split(path, AntPathMatcher.DEFAULT_PATH_SEPARATOR)[0];

        final List<RestfulMock> mocks = entityManager.createQuery("FROM RestfulMock rm "
                + " WHERE rm.method = :method "
                + " AND rm.mockType IN (:mockTypes) "
                + " AND (rm.path = :path1 OR rm.path LIKE '/'||:path2||'%')"
                + " AND rm.createdBy.role = :role "
                + " AND rm.status = 'ACTIVE'", RestfulMock.class)
                .setParameter("method", method)
                .setParameter("mockTypes", mockTypes)
                .setParameter("path1", path)
                .setParameter("path2", part1)
                .setParameter("role", SmockinUserRoleEnum.SYS_ADMIN)
                .getResultList();

        return matchPath(mocks, path, false);
    }

    @Override
    public RestfulMock findActiveByMethodAndPathPatternAndTypesForMultiUser(final RestMethodEnum method, final String path, final List<RestMockTypeEnum> mockTypes) {

        final String part1 = StringUtils.split(path, AntPathMatcher.DEFAULT_PATH_SEPARATOR)[0];

        final List<RestfulMock> mocks = entityManager.createQuery("FROM RestfulMock rm "
                + " WHERE rm.method = :method "
                + " AND rm.mockType IN (:mockTypes) "
                + " AND "
                + " ( "
                + " ('/'||rm.createdBy.ctxPath||rm.path = :path1 OR '/'||rm.createdBy.ctxPath||rm.path LIKE '/'||:path2||'%') "
                + " OR "
                + " (rm.path = :path1 OR rm.path LIKE '/'||:path2||'%') "
                + " ) "
                + " AND rm.status = 'ACTIVE'", RestfulMock.class)
                .setParameter("method", method)
                .setParameter("mockTypes", mockTypes)
                .setParameter("path1", path)
                .setParameter("path2", part1)
                .getResultList();

        return matchPath(mocks, path, true);
    }

    private RestfulMock matchPath(final List<RestfulMock> mocks, final String path, final boolean matchOnUserCtxPath) {

        if (mocks.isEmpty()) {
            return null;
        }

        final AntPathMatcher matcher = new AntPathMatcher(AntPathMatcher.DEFAULT_PATH_SEPARATOR);

        return mocks.stream()
                .filter(m ->
                    matcher.match(buildMockMatchingPath(m, matchOnUserCtxPath), path))
                .findFirst()
                .orElse(null);
    }

    private String buildMockMatchingPath(final RestfulMock m, final boolean matchOnUserCtxPath) {
        return (matchOnUserCtxPath && !SmockinUserRoleEnum.SYS_ADMIN.equals(m.getCreatedBy().getRole()))
                ? "/" + m.getCreatedBy().getCtxPath() + m.getPath()
                : m.getPath();
    }

}
