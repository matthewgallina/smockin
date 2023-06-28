package com.smockin.admin.service;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.smockin.admin.dto.TunnelRequestDTO;
import com.smockin.admin.dto.response.TunnelResponseDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.TunnelException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicReference;

@Service
@Transactional
public class TunnelServiceImpl implements TunnelService {

    private SmockinUserService smockinUserService;
    private UserTokenServiceUtils userTokenServiceUtils;
    private MockedServerEngineService mockedServerEngineService;

    private final AtomicReference<NgrokClient> ngrokClientRef = new AtomicReference();


    @Autowired
    public TunnelServiceImpl(final SmockinUserService smockinUserService,
                             final UserTokenServiceUtils userTokenServiceUtils,
                             final MockedServerEngineService mockedServerEngineService) {
        this.smockinUserService = smockinUserService;
        this.userTokenServiceUtils = userTokenServiceUtils;
        this.mockedServerEngineService = mockedServerEngineService;
    }

    @Override
    public TunnelResponseDTO load(final String token) {

        final NgrokClient ngrokClient = getNgrokClientInstance();

        if (ngrokClient == null) {
            return new TunnelResponseDTO(false, null);
        }

        final boolean isRunning = ngrokClient.getNgrokProcess().isRunning();

        if (isRunning && ngrokClient.getTunnels().isEmpty()) {
            throw new TunnelException("ngrok Tunnel is missing");
        }
        if (isRunning && ngrokClient.getTunnels().size() > 1) {
            throw new TunnelException("Multiple ngrok tunnels were found");
        }

        final String uri = (isRunning)
                ? ngrokClient.getTunnels().get(0).getPublicUrl()
                : null;

        return new TunnelResponseDTO(isRunning, uri);
    }

    @Override
    public TunnelResponseDTO update(final TunnelRequestDTO dto,
                                    final String token) throws AuthException, ValidationException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

        NgrokClient ngrokClient = getNgrokClientInstance();

        if (ngrokClient == null) {
            ngrokClientRef.set(ngrokClient = instanceNgrokClient());
        }

        if (!dto.isEnabled()) {

            if (ngrokClient.getNgrokProcess().isRunning()) {
                ngrokClient.kill();
            }

            return new TunnelResponseDTO(ngrokClient.getNgrokProcess().isRunning(), null);
        }

        if (ngrokClient.getNgrokProcess().isRunning()) {
            // Already running
            return new TunnelResponseDTO(true, ngrokClient.getTunnels().get(0).getPublicUrl());
        }

        final MockedServerConfigDTO mockedServerConfig = mockedServerEngineService.loadServerConfig(ServerTypeEnum.RESTFUL);
        final String authToken = mockedServerConfig.getNativeProperties().get(GeneralUtils.NGROK_AUTH_TOKEN);

        if (StringUtils.isBlank(authToken)) {
            throw new ValidationException("Unable to start ngrok. Could not locate an Auth Token in Server Config.");
        }

        final MockServerState serverState = mockedServerEngineService.getRestServerState();

        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr(serverState.getPort())
                .build();

        ngrokClient.setAuthToken(authToken);

        final Tunnel httpTunnel = ngrokClient.connect(createTunnel);

        return new TunnelResponseDTO(ngrokClient.getNgrokProcess().isRunning(), httpTunnel.getPublicUrl());
    }

    NgrokClient instanceNgrokClient() {
        return new NgrokClient.Builder().build();
    }

    NgrokClient getNgrokClientInstance() {
        return ngrokClientRef.get();
    }

}
