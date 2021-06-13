package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.ProxyForwardUserConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Created by mgallina.
 */
public interface ProxyForwardUserConfigDAO extends JpaRepository<ProxyForwardUserConfig, Long> {

    @Query("FROM ProxyForwardUserConfig pfuc WHERE pfuc.createdBy.id = :userId")
    ProxyForwardUserConfig findByUser(@Param("userId") final long userId);

}
