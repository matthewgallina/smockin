package com.smockin.admin.service;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.smockin.admin.dto.TunnelRequestDTO;
import com.smockin.admin.dto.response.TunnelResponseDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockServerState;
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

    private AtomicReference<NgrokClient> ngrokClientRef = new AtomicReference();


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

        final NgrokClient ngrokClient = ngrokClientRef.get();

        if (ngrokClient == null) {
            return new TunnelResponseDTO(false, null);
        }

        final boolean isRunning = ngrokClient.getNgrokProcess().isRunning();

        final String uri = (isRunning)
                ? ngrokClient.getTunnels().get(0).getPublicUrl()
                : null;

        return new TunnelResponseDTO(isRunning, uri);
    }

    @Override
    public TunnelResponseDTO update(final TunnelRequestDTO dto,
                                    final String token) throws AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

        final MockServerState serverState = mockedServerEngineService.getRestServerState();

        NgrokClient ngrokClient = ngrokClientRef.get();

        if (ngrokClient == null) {
            ngrokClientRef.set(ngrokClient = new NgrokClient.Builder().build());
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

        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr(serverState.getPort())
                .build();

        final Tunnel httpTunnel = ngrokClient.connect(createTunnel);

        return new TunnelResponseDTO(ngrokClient.getNgrokProcess().isRunning(), httpTunnel.getPublicUrl());
    }

}
