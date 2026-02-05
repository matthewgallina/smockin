package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.Identifier;
import jakarta.persistence.Query;

/**
 * Created by gallina.
 */
public interface MigrationDAO {

    Query buildQuery(final String sql);
    Query buildNativeQuery(final String sql);
    <E extends Identifier> void persist(final E e);

}
