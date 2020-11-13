package com.smockin.mockserver.engine;

import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.exception.MockServerException;

/**
 * Created by mgallina.
 */
public interface BaseServerEngine<C, M> {

    void start(final C c, final M m) throws MockServerException;
    MockServerState getCurrentState() throws MockServerException;
    void shutdown() throws MockServerException;

}
