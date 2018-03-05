package com.smockin.admin.service;

import com.smockin.admin.dto.FtpMockDTO;
import com.smockin.admin.dto.response.FtpMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.FtpMockDAO;
import com.smockin.admin.persistence.entity.FtpMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public String createEndpoint(final FtpMockDTO dto) {
        logger.debug("createEndpoint called");

        return ftpMockDAO.save(new FtpMock(dto.getName(), dto.getStatus()))
                .getExtId();
    }

    @Override
    public void updateEndpoint(final String mockExtId, final FtpMockDTO dto) throws RecordNotFoundException {
        logger.debug("updateEndpoint called");

        final FtpMock mock = loadFtpMock(mockExtId);

        mock.setName(dto.getName());
        mock.setStatus(dto.getStatus());

        ftpMockDAO.save(mock);
    }

    @Override
    public void deleteEndpoint(final String mockExtId) throws RecordNotFoundException {
        logger.debug("deleteEndpoint called");

        ftpMockDAO.detach(loadFtpMock(mockExtId));
    }

    @Override
    public List<FtpMockResponseDTO> loadAll() {
        logger.debug("loadAll called");

        return ftpMockDAO.findAll()
                .stream()
                .map(e -> new FtpMockResponseDTO(e.getExtId(), e.getName(), e.getStatus(), e.getDateCreated()))
                .collect(Collectors.toList());
    }

    FtpMock loadFtpMock(final String mockExtId) throws RecordNotFoundException {

        final FtpMock mock = ftpMockDAO.findByExtId(mockExtId);

        if (mock == null) {
            throw new RecordNotFoundException();
        }

        return mock;
    }

}
