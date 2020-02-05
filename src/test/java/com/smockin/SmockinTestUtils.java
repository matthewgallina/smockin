package com.smockin;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.mock.web.MockMultipartFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by mgallina on 30/08/17.
 */
public final class SmockinTestUtils {

    public static RestfulMock buildRestfulMock(final String path, final RestMockTypeEnum mockType, final int initOrder, final RestMethodEnum restMethod, final RecordStatusEnum status, final SmockinUser user) {

        final RestfulMock mock = new RestfulMock();
        mock.setPath(path);
        mock.setMockType(mockType);
        mock.setInitializationOrder(initOrder);
        mock.setMethod(restMethod);
        mock.setStatus(status);
        mock.setCreatedBy(user);

        return mock;
    }

    public static SmockinUser buildSmockinUser() {
        return new SmockinUser("admin", "letmein", "admin", "admin", SmockinUserRoleEnum.SYS_ADMIN, RecordStatusEnum.ACTIVE, GeneralUtils.generateUUID(), GeneralUtils.generateUUID());
    }

    public MockMultipartFile buildMockMultiPartFile(final String fileName) throws URISyntaxException, IOException {

        final URL openApiUrl = this.getClass().getClassLoader().getResource(fileName);
        final File apiFile = new File(openApiUrl.toURI());
        final FileInputStream openInput = new FileInputStream(apiFile);

        return new MockMultipartFile(fileName, apiFile.getName(), "text/plain", IOUtils.toByteArray(openInput));
    }

}
