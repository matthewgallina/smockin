package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.FtpMockDAO;
import com.smockin.admin.persistence.entity.FtpMock;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import org.apache.commons.io.FileUtils;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@Service
public class MockedFtpServerEngine implements MockServerEngine<MockedServerConfigDTO, List<FtpMock>> {

    private final Logger logger = LoggerFactory.getLogger(MockedFtpServerEngine.class);

    private final String ftpHomeDir = System.getProperty("user.home") + File.separator + ".smockin/ftp" + File.separator;

    private UserManager userManager = null; // not sure this is thread safe, so handling in synchronised block
    private FtpServer server = null;        // not sure this is thread safe, so handling in synchronised block

    private final Object monitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);

    @Autowired
    private FtpMockDAO ftpMockDAO;

    /*
    public static void main(String[] args) {

        final MockedServerConfigDTO config = new MockedServerConfigDTO();
        config.setPort(3550);

        try {
            new MockedFtpServerEngine().start(config, new ArrayList<FtpMock>());
        } catch (MockServerException e) {
            e.printStackTrace();
        }

    }
*/

    @Override
    public void start(final MockedServerConfigDTO config, final List<FtpMock> data) throws MockServerException {

        // Invoke all lazily loaded data and detach entity.
        invokeAndDetachData(data);

        // Build FTP server
        initServerConfig(config.getPort());

        // Define FTP users
        buildFTPUsers(data);

        // Start FTP Server
        initServer(config.getPort());

    }

    public MockServerState getCurrentState() throws MockServerException {
        synchronized (monitor) {
            return serverState;
        }
    }

    @Override
    public void shutdown() throws MockServerException {

        try {

            synchronized (monitor) {

                server.stop();

                serverState.setRunning(false);
            }

        } catch (Throwable ex) {
            throw new MockServerException(ex);
        }

    }

    void initServerConfig(final int port) throws MockServerException {
        logger.debug("initServerConfig called");

        try {

            synchronized (monitor) {

                final FtpServerFactory serverFactory = new FtpServerFactory();
                final ListenerFactory factory = new ListenerFactory();
                factory.setPort(port);
                serverFactory.addListener("default", factory.createListener());

                // Create users
                final PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
                userManager = userManagerFactory.createUserManager();
                serverFactory.setUserManager(userManager);

                server = serverFactory.createServer();
            }

        } catch (Throwable ex) {
            throw new MockServerException(ex);
        }

    }

    void initServer(final int port) throws MockServerException {
        logger.debug("initServer called");

        try {

            synchronized (monitor) {

                if (server != null)
                    server.start();

                serverState.setRunning(true);
                serverState.setPort(port);
            }

        } catch (Throwable ex) {
            throw new MockServerException(ex);
        }

    }

    @Transactional
    void invokeAndDetachData(final List<FtpMock> mocks) {

        for (FtpMock mock : mocks) {

            // Important!
            // Detach all JPA entity beans from EntityManager Context, so they can be
            // continually accessed again here as a simple data bean
            // within each request to the mocked FTP endpoint.
            ftpMockDAO.detach(mock);
        }

    }

    // Expects FtpMock to be detached
    void buildFTPUsers(final List<FtpMock> mocks) throws MockServerException {
        logger.debug("buildFTPDestinations called");

        for (FtpMock m : mocks) {
            buildUser(m.getName(), m.getName(), m.getName());
        }

    }


    void buildUser(final String username, final String password, final String userHomeDir) throws MockServerException {
        logger.debug("buildUser called");

        final BaseUser ftpBaseUser = new BaseUser();

        ftpBaseUser.setName(username);
        ftpBaseUser.setPassword(password);
        ftpBaseUser.setEnabled(true);
        ftpBaseUser.setAuthorities(new ArrayList<Authority>() {
            {
                add(new WritePermission());
                add(new ConcurrentLoginPermission(0, 0)); // unlimited
            }
        });

        handleUserDirCreation(userHomeDir);

        ftpBaseUser.setHomeDirectory(ftpHomeDir + userHomeDir);
        ftpBaseUser.setMaxIdleTime(0);

        try {
            synchronized (monitor) {
                userManager.save(ftpBaseUser);
            }
        } catch (FtpException e) {
            throw new MockServerException("Error adding ftp user " + username, e);
        }
    }

    private void handleUserDirCreation(final String username) throws MockServerException {

        final File f = new File(ftpHomeDir + username);

        try {
            if (!f.exists() || !f.isDirectory()) {
                f.mkdirs();
            }
        } catch (Throwable ex) {
            throw new MockServerException("Error creating FTP home dir for user: " + username, ex);
        }

    }

}
