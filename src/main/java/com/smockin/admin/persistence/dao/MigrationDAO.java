package com.smockin.admin.persistence.dao;

import javax.persistence.Query;

/**
 * Created by gallina.
 */
public interface MigrationDAO {

    Query buildQuery(final String sql);
    Query buildNativeQuery(final String sql);

}
