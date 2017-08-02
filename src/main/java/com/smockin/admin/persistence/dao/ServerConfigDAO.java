package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by mgallina.
 */
public interface ServerConfigDAO extends JpaRepository<ServerConfig, Long> {

    ServerConfig findByServerType(final ServerTypeEnum serverType);

}
