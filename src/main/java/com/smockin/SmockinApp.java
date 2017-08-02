package com.smockin;

import com.smockin.admin.persistence.CoreDataSet;
import com.smockin.admin.service.MockedServerEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.annotation.PostConstruct;

/**
 * Created by mgallina.
 */
@SpringBootApplication
@ComponentScan({ "com.smockin.admin", "com.smockin.mockserver" })
@EnableJpaRepositories("com.smockin.admin.persistence.dao")
@EntityScan("com.smockin.admin.persistence.entity")
class SmockinConfig {

    @Autowired
    private CoreDataSet coreDataSet;

    @Autowired
    private MockedServerEngineService mockedServerEngineService;

    @PostConstruct
    public void after() {

        coreDataSet.exec();

        mockedServerEngineService.handleServerAutoStart();

    }

}

public class SmockinApp {

    public static void main(String[] args) {
        SpringApplication.run(SmockinConfig.class, args);
    }



}
