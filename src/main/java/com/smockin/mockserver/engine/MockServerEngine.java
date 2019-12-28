package com.smockin.mockserver.engine;

import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;

/**
 * Created by mgallina.
 */
public interface MockServerEngine<C extends MockedServerConfigDTO> extends BaseServerEngine<C> {

    void start(final C config) throws MockServerException;

}
