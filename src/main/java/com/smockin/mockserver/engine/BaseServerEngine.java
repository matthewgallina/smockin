package com.smockin.mockserver.engine;

import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.exception.MockServerException;

/**
 * Created by mgallina.
 */
public interface BaseServerEngine<C, D> {

    void start(final C config, D data) throws MockServerException;
    MockServerState getCurrentState() throws MockServerException;
    void shutdown() throws MockServerException;

}
