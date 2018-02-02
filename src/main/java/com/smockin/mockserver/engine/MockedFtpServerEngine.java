package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.FtpMockDAO;
import com.smockin.admin.persistence.entity.FtpMock;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@Service
public class MockedFtpServerEngine implements MockServerEngine<MockedServerConfigDTO, List<FtpMock>> {

    private final Logger logger = LoggerFactory.getLogger(MockedFtpServerEngine.class);

    @Autowired
    private FtpMockDAO ftpMockDAO;

    private UserManager userManager = null; // not sure this is thread safe
    private FtpServer server = null;        // not sure this is thread safe

    private final Object monitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);

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
        initServerConfig(config);

        // Define FTP directories
        buildFTPDestinations(data);

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

    void initServerConfig(final MockedServerConfigDTO config) throws MockServerException {
        logger.debug("initServerConfig called");

        try {

            synchronized (monitor) {

                final FtpServerFactory serverFactory = new FtpServerFactory();
                final ListenerFactory factory = new ListenerFactory();
                factory.setPort(config.getPort());
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
    void buildFTPDestinations(final List<FtpMock> mocks) throws MockServerException {
        logger.debug("buildFTPDestinations called");

        buildUsers();

    }

    void buildUsers() throws MockServerException {

        buildUser("fred", "letmein", "/foo");

    }

    void buildUser(final String username, final String password, final String homeDir) throws MockServerException {
        logger.debug("buildUser called");

        final BaseUser user = new BaseUser();

        user.setName(username);
        user.setPassword(password);
        user.setEnabled(true);
        user.setAuthorities(new ArrayList<Authority>() {
            {
                new WritePermission();
            }
        });

        user.setHomeDirectory(homeDir);
        user.setMaxIdleTime(0);

        try {
            synchronized (monitor) {
                userManager.save(user);
            }
        } catch (FtpException e) {
            throw new MockServerException("Error adding ftp user " + username, e);
        }
    }

}
