package com.smockin;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Created by mgallina.
 */
@ComponentScan(value = { "com.smockin.admin", "com.smockin.mockserver" })
@EnableJpaRepositories("com.smockin.admin.persistence.dao")
@EntityScan("com.smockin.admin.persistence.entity")
public class SmockinTestConfig {

}