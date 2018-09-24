package com.smockin.mockserver.engine;

import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;

/**
 * Created by mgallina.
 */
public interface MockServerEngine<C extends MockedServerConfigDTO, D> extends BaseServerEngine<C, D> {

    void start(final C config, D data) throws MockServerException;

}
