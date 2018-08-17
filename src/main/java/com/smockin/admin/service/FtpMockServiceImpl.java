package com.smockin.admin.service;

import com.smockin.admin.dto.FtpMockDTO;
import com.smockin.admin.dto.response.FtpMockResponseDTO;
import com.smockin.admin.enums.SearchFilterEnum;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.FtpMockDAO;
import com.smockin.admin.persistence.entity.FtpMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mgallina.
 */
@Service
@Transactional
public class FtpMockServiceImpl implements FtpMockService {

    private final Logger logger = LoggerFactory.getLogger(FtpMockServiceImpl.class);

    @Autowired
    private FtpMockDAO ftpMockDAO;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Value("${smockin.ftp.root.dir}")
    private String ftpHomeDir;


    @Override
    public String createEndpoint(final FtpMockDTO dto, final String token) throws RecordNotFoundException {
        logger.debug("createEndpoint called");

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentUser(token);

        final FtpMock mock = ftpMockDAO.save(new FtpMock(dto.getName(), dto.getStatus(), smockinUser));

        new File(buildUserBaseDir(mock)).mkdir();

        return mock.getExtId();
    }

    @Override
    public void updateEndpoint(final String mockExtId, final FtpMockDTO dto, final String token) throws RecordNotFoundException, ValidationException {
        logger.debug("updateEndpoint called");

        final FtpMock mock = loadFtpMock(mockExtId);

        userTokenServiceUtils.validateRecordOwner(mock.getCreatedBy(), token);

        final String originalName = buildUserBaseDir(mock);

        mock.setName(dto.getName());
        mock.setStatus(dto.getStatus());

        ftpMockDAO.save(mock);

        // Rename user's ftp dir.
        // Any runtime exception will cause transaction (rename above) to rollback.
        new File(originalName).renameTo(new File(buildUserBaseDir(mock)));

    }

    @Override
    public void deleteEndpoint(final String mockExtId, final String token) throws RecordNotFoundException, IOException, ValidationException {
        logger.debug("deleteEndpoint called");

        final FtpMock mock = loadFtpMock(mockExtId);

        userTokenServiceUtils.validateRecordOwner(mock.getCreatedBy(), token);

        final String ftpPathToRemove = buildUserBaseDir(mock);

        ftpMockDAO.delete(mock);

        FileUtils.deleteDirectory(new File(ftpPathToRemove));
    }

    @Override
    public List<FtpMockResponseDTO> loadAll(final String searchFilter, final String token) throws RecordNotFoundException {
        logger.debug("loadAll called");

        final List<FtpMock> mocks;

        if (SearchFilterEnum.ALL.name().equalsIgnoreCase(searchFilter)) {
            mocks = ftpMockDAO.findAll();
        } else {
            mocks = ftpMockDAO.findAllByUser(userTokenServiceUtils.loadCurrentUser(token).getId());
        }

        return mocks
                .stream()
                .map(e -> new FtpMockResponseDTO(e.getExtId(), e.getName(), e.getStatus(), e.getDateCreated()))
                .collect(Collectors.toList());
    }

    @Override
    public void uploadFile(final String mockExtId, final MultipartFile inboundFile) throws RecordNotFoundException, IOException {
        logger.debug("uploadFile called");

        final FtpMock mock = loadFtpMock(mockExtId);

        final String destFileURI = buildUserBaseDir(mock)
                + File.separator
                + inboundFile.getOriginalFilename();

        if (logger.isDebugEnabled()) {
            logger.debug("Saving file: " + destFileURI);
        }

        FileUtils.copyInputStreamToFile(inboundFile.getInputStream(), new File(destFileURI));
    }

    @Override
    public List<String> loadUploadFiles(final String mockExtId) throws RecordNotFoundException, IOException {
        logger.debug("loadUploadFiles called");

        final FtpMock mock = loadFtpMock(mockExtId);

        final String ftpUserHomeURI = buildUserBaseDir(mock);

        return Files.walk(Paths.get(ftpUserHomeURI))
            .filter(e -> !ftpUserHomeURI.equals(e.toString()))
            .map(e -> (e.toString().replaceFirst(ftpUserHomeURI + File.separator, "")
                        + ((Files.isDirectory(e)) ? "/" : "")))
            .collect(Collectors.toList());
    }

    @Override
    public void deleteUploadedFile(final String mockExtId, final String uri) throws RecordNotFoundException, ValidationException, IOException {
        logger.debug("deleteUploadedFile called");

        if (uri == null) {
            throw new ValidationException("file uri is required");
        }

        final FtpMock mock = loadFtpMock(mockExtId);

        final String ftpUserHomeURI = buildUserBaseDir(mock)
                + File.separator + uri;

        final File file = new File(ftpUserHomeURI);

        if (!file.exists()) {
            logger.error("Unable to locate file on system: " + ftpUserHomeURI);
            throw new RecordNotFoundException();
        }

        if (file.isDirectory()) {
            FileUtils.deleteDirectory(file);
        } else {
            file.delete();
        }

    }

    FtpMock loadFtpMock(final String mockExtId) throws RecordNotFoundException {
        logger.debug("loadFtpMock called");

        final FtpMock mock = ftpMockDAO.findByExtId(mockExtId);

        if (mock == null) {
            throw new RecordNotFoundException();
        }

        return mock;
    }

    String buildUserBaseDir(final FtpMock mock) {

        return ftpHomeDir
                + (SmockinUserRoleEnum.SYS_ADMIN.equals(mock.getCreatedBy().getRole())
                    ? "admin" + File.separator + mock.getName()
                    : mock.getCreatedBy().getCtxPath() + File.separator + mock.getName());
    }

}
