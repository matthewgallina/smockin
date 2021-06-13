package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;
import com.smockin.admin.persistence.dao.ProxyForwardUserConfigDAO;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.ProxyForwardUserConfig;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.enums.ProxyModeTypeEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by gallina.
 */
@Component
public class MigrationPatch_2160 implements MigrationPatch {

    @Autowired
    private MigrationDAO migrationDAO;

    @Autowired
    private ServerConfigDAO serverConfigDAO;

    @Autowired
    private ProxyForwardUserConfigDAO proxyForwardUserConfigDAO;

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    @Override
    public String versionNo() {
        return "2.16.0";
    }

    @Override
    public void execute() {

        //
        // Copy existing proxy config 'PROXY_MODE_TYPE' & 'NO_FORWARD_WHEN_404_MOCK' columns into new 'PROXY_FORWARD_USER_CONFIG' table
        final Object object = migrationDAO.buildNativeQuery("SELECT PROXY_MODE_TYPE, NO_FORWARD_WHEN_404_MOCK "
                + " FROM SERVER_CONFIG "
                + " WHERE SERVER_TYPE = :serverType")
                .setParameter("serverType", ServerTypeEnum.RESTFUL.name())
                .getSingleResult();

        final ProxyModeTypeEnum proxyModeType;
        final boolean doNotForwardWhen404Mock;

        if (object != null
                && object instanceof Object[]
                && ((Object[])object).length == 2) {

            final Object[] fields = (Object[]) object;
            proxyModeType = ProxyModeTypeEnum.valueOf((String)fields[0]);
            doNotForwardWhen404Mock = (boolean)fields[1];

        } else {
            proxyModeType = null;
            doNotForwardWhen404Mock = false;
        }


        final ServerConfig serverConfig = serverConfigDAO.findByServerType(ServerTypeEnum.RESTFUL);

        ProxyForwardUserConfig proxyForwardUserConfig = new ProxyForwardUserConfig();
        proxyForwardUserConfig.setServerConfig(serverConfig);
        proxyForwardUserConfig.setProxyModeType(proxyModeType);
        proxyForwardUserConfig.setDoNotForwardWhen404Mock(doNotForwardWhen404Mock);
        proxyForwardUserConfig.setCreatedBy(smockinUserDAO.findAllByRole(SmockinUserRoleEnum.SYS_ADMIN).get(0));

        proxyForwardUserConfig = proxyForwardUserConfigDAO.save(proxyForwardUserConfig);


        //
        // Update new FK 'PROXY_FORWARD_USER_CONFIG_ID' column with the ID from the new 'PROXY_FORWARD_USER_CONFIG' table
        migrationDAO.buildNativeQuery("ALTER TABLE PROXY_FORWARD_MAPPING "
                + " ADD COLUMN PROXY_FORWARD_USER_CONFIG_ID BIGINT(19)")
                .executeUpdate();

        migrationDAO.buildNativeQuery("ALTER TABLE PROXY_FORWARD_MAPPING "
                + " ADD FOREIGN KEY (PROXY_FORWARD_USER_CONFIG_ID) "
                + " REFERENCES PROXY_FORWARD_USER_CONFIG(ID)")
                .executeUpdate();

        migrationDAO.buildNativeQuery("UPDATE PROXY_FORWARD_MAPPING "
                + " SET PROXY_FORWARD_USER_CONFIG_ID = :proxyForwardUserConfigId")
                .setParameter("proxyForwardUserConfigId", proxyForwardUserConfig.getId())
                .executeUpdate();

        migrationDAO.buildNativeQuery("ALTER TABLE PROXY_FORWARD_MAPPING "
                + " ALTER COLUMN PROXY_FORWARD_USER_CONFIG_ID BIGINT(19) NOT NULL ")
                .executeUpdate();


        //
        // Drop old 'PROXY_MODE_TYPE' & 'NO_FORWARD_WHEN_404_MOCK' columns
        migrationDAO.buildNativeQuery("ALTER TABLE SERVER_CONFIG "
                + " DROP COLUMN PROXY_MODE_TYPE;")
                .executeUpdate();

        migrationDAO.buildNativeQuery("ALTER TABLE SERVER_CONFIG "
                + " DROP COLUMN NO_FORWARD_WHEN_404_MOCK;")
                .executeUpdate();

    }

}
