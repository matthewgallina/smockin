package com.smockin.admin.service;

import com.smockin.admin.dto.JmsMockDTO;
import com.smockin.admin.dto.response.JmsMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.JmsMockDAO;
import com.smockin.admin.persistence.entity.JmsMock;
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
public class JmsMockServiceImpl implements JmsMockService {

    private final Logger logger = LoggerFactory.getLogger(JmsMockServiceImpl.class);

    @Autowired
    private JmsMockDAO jmsMockDAO;

    @Override
    public String createEndpoint(final JmsMockDTO dto) {
        logger.debug("createEndpoint called");

        return jmsMockDAO.save(new JmsMock(dto.getName(), dto.getJmsMockType(), dto.getStatus()))
                .getExtId();
    }

    @Override
    public void updateEndpoint(final String mockExtId, final JmsMockDTO dto) throws RecordNotFoundException {
        logger.debug("updateEndpoint called");

        final JmsMock mock = loadJmsMock(mockExtId);

        mock.setName(dto.getName());
        mock.setStatus(dto.getStatus());

        jmsMockDAO.save(mock);
    }

    @Override
    public void deleteEndpoint(final String mockExtId) throws RecordNotFoundException {
        logger.debug("deleteEndpoint called");

        jmsMockDAO.delete(loadJmsMock(mockExtId));
    }

    @Override
    public List<JmsMockResponseDTO> loadAll() {
        logger.debug("loadAll called");

        return jmsMockDAO.findAll()
                .stream()
                .map(e -> new JmsMockResponseDTO(e.getExtId(), e.getName(), e.getStatus(), e.getJmsType(), e.getDateCreated()))
                .collect(Collectors.toList());
    }

    JmsMock loadJmsMock(final String mockExtId) throws RecordNotFoundException {

        final JmsMock mock = jmsMockDAO.findByExtId(mockExtId);

        if (mock == null) {
            throw new RecordNotFoundException();
        }

        return mock;
    }

}
