package com.smockin.mockserver.engine;

import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;

/**
 * Created by mgallina.
 */
public interface MockServerEngine<C extends MockedServerConfigDTO, M>
        extends BaseServerEngine<C, M> {

    void start(final C config, final M m) throws MockServerException;

}
