package com.smockin;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Created by mgallina.
 */
@ComponentScan(value = { "com.smockin.admin", "com.smockin.mockserver" },
                excludeFilters = @ComponentScan.Filter(type = FilterType.ASPECTJ, pattern = "com.smockin.admin.websocket.*"))
@EnableJpaRepositories("com.smockin.admin.persistence.dao")
@EntityScan("com.smockin.admin.persistence.entity")
public class SmockinTestConfig {

}
