package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.ProxyForwardMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProxyForwardMappingDAO extends JpaRepository<ProxyForwardMapping, Long> {

}
