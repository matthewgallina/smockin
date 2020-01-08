package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by gallina.
 */
@Component
public class MigrationPatch_210 implements MigrationPatch {

    @Autowired
    private RestfulMockServiceUtils restfulMockServiceUtils;

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Override
    public String versionNo() {
        return "2.1.0";
    }

    @Override
    public void execute() {

        restfulMockDAO.findAll()
                .stream()
                .forEach(m -> {
            m.setPath(restfulMockServiceUtils.formatInboundPathVarArgs(m.getPath()));
            restfulMockDAO.save(m);
        });

        restfulMockDAO.flush();
    }

}
