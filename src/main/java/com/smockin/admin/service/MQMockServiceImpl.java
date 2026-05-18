package com.smockin.admin.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.MQMockDAO;
import com.smockin.admin.persistence.dao.MQMockMessageDAO;
import com.smockin.admin.persistence.entity.MQMock;
import com.smockin.admin.persistence.entity.MQMockMessage;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.MQTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MQMockDTO;
import com.smockin.mockserver.dto.MQMockMessageDTO;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.engine.MockedMQServerEngine;
import com.smockin.utils.GeneralUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class MQMockServiceImpl implements MQMockService {

    private final Logger logger = LoggerFactory.getLogger(MQMockServiceImpl.class);

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private MQMockDAO mqMockDAO;

    @Autowired
    private MQMockMessageDAO mqMockMessageDAO;

    @Autowired
    private MockedMQServerEngine mockedMQServerEngine;


    @Override
    public List<MQMockDTO> loadAll(final String token) throws RecordNotFoundException {
        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        return mqMockDAO.findAllByUser(smockinUser.getId())
                .stream()
                .map(this::toMQMockDTO)
                .collect(Collectors.toList());
    }

    @Override
    public MQMockDTO loadById(final String externalId, final String token) throws RecordNotFoundException {
        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
        final MQMock mqMock = loadById(externalId, smockinUser);
        return toMQMockDTO(mqMock);
    }

    @Override
    public String create(final MQMockDTO mqMockDTO, final String token) throws RecordNotFoundException, ValidationException {
        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        validateMQMockDTO(mqMockDTO);

        // Check if name already exists
        if (mqMockDAO.findByName(mqMockDTO.getName()) != null) {
            throw new ValidationException("MQ Mock with name '" + mqMockDTO.getName() + "' already exists");
        }

        MQTypeEnum mqType = MQTypeEnum.toMQType(mqMockDTO.getMqType());
        if (mqType == null) {
            throw new ValidationException("Invalid MQ type: " + mqMockDTO.getMqType());
        }

        RecordStatusEnum status = StringUtils.isNotBlank(mqMockDTO.getStatus())
                ? RecordStatusEnum.valueOf(mqMockDTO.getStatus())
                : RecordStatusEnum.ACTIVE;

        MQMock mqMock = new MQMock(
                mqMockDTO.getName(),
                mqType,
                mqMockDTO.getDestinationName(),
                mqMockDTO.isTopic(),
                status,
                smockinUser,
                mqMockDTO.isSaveMessages()
        );

        mqMock.setBrokerUrl(mqMockDTO.getBrokerUrl());
        mqMock.setProperties(mqMockDTO.getProperties() != null ? mqMockDTO.getProperties() : Map.of());

        String extId = mqMockDAO.save(mqMock).getExtId();

        logger.info("Created MQ Mock: {} with extId: {}", mqMockDTO.getName(), extId);
        return extId;
    }

    @Override
    public void update(final String externalId, final MQMockDTO mqMockDTO, final String token) throws RecordNotFoundException, ValidationException {
        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        validateMQMockDTO(mqMockDTO);

        MQMock mqMock = loadById(externalId, smockinUser);

        // Update fields
        mqMock.setName(mqMockDTO.getName());
        mqMock.setDestinationName(mqMockDTO.getDestinationName());
        mqMock.setTopic(mqMockDTO.isTopic());
        mqMock.setBrokerUrl(mqMockDTO.getBrokerUrl());
        mqMock.setSaveMessages(mqMockDTO.isSaveMessages());

        if (StringUtils.isNotBlank(mqMockDTO.getMqType())) {
            MQTypeEnum mqType = MQTypeEnum.toMQType(mqMockDTO.getMqType());
            if (mqType != null) {
                mqMock.setMqType(mqType);
            }
        }

        if (StringUtils.isNotBlank(mqMockDTO.getStatus())) {
            mqMock.setStatus(RecordStatusEnum.valueOf(mqMockDTO.getStatus()));
        }

        if (mqMockDTO.getProperties() != null) {
            mqMock.getProperties().clear();
            mqMock.getProperties().putAll(mqMockDTO.getProperties());
        }

        mqMockDAO.save(mqMock);

        logger.info("Updated MQ Mock: {}", externalId);
    }

    @Override
    public void delete(final String externalId, final String token) throws RecordNotFoundException {
        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
        final MQMock mqMock = loadById(externalId, smockinUser);

        // Remove from engine if running
        mockedMQServerEngine.removeMQMock(externalId);

        // Delete from database
        mqMockDAO.delete(mqMock);

        logger.info("Deleted MQ Mock: {}", externalId);
    }

    @Override
    public MockServerState getMQServerState() {
        try {
            return mockedMQServerEngine.getCurrentState();
        } catch (Exception e) {
            logger.error("Error getting MQ server state", e);
            return new MockServerState(false, 0);
        }
    }

    @Override
    public void startMQServer(final String token) throws RecordNotFoundException, ValidationException {
        userTokenServiceUtils.loadCurrentActiveUser(token);
        // Engine will be started with all active mocks
        logger.info("MQ Server started");
    }

    @Override
    public void stopMQServer(final String token) throws RecordNotFoundException, ValidationException {
        userTokenServiceUtils.loadCurrentActiveUser(token);
        try {
            mockedMQServerEngine.shutdown();
            logger.info("MQ Server stopped");
        } catch (Exception e) {
            logger.error("Error stopping MQ server", e);
        }
    }

    @Override
    public List<MQMockMessageDTO> loadMessages(final String mqMockExtId, final String token) throws RecordNotFoundException {
        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        // Verify user has access to this MQ Mock
        MQMock mqMock = loadById(mqMockExtId, smockinUser);

        // Load messages from database
        List<MQMockMessage> messages = mqMockMessageDAO.findByMqMockExtId(mqMockExtId);

        return messages.stream()
                .map(this::toMQMockMessageDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void sendMessage(final String mqMockExtId, final String messageBody, final String messageKey,
                           final String contentType, final String headers, final String token)
            throws RecordNotFoundException, ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
        loadById(mqMockExtId, smockinUser); // Verify access

        if (StringUtils.isBlank(messageBody)) {
            throw new ValidationException("Message body is required");
        }

        // Parse headers JSON if provided
        Map<String, String> headersMap = null;
        if (StringUtils.isNotBlank(headers)) {
            try {
                headersMap = GeneralUtils.deserialiseJson(headers, new TypeReference<Map<String, String>>(){});
            } catch (Exception e) {
                throw new ValidationException("Invalid headers JSON format");
            }
        }

        // Send message via engine
        mockedMQServerEngine.sendMessage(mqMockExtId, messageBody, messageKey, contentType, headersMap);

        logger.info("Message sent to MQ Mock: {}", mqMockExtId);
    }

    @Override
    public void clearMessages(final String mqMockExtId, final String token) throws RecordNotFoundException {
        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
        loadById(mqMockExtId, smockinUser); // Verify access

        mqMockMessageDAO.deleteByMqMockExtId(mqMockExtId);

        logger.info("Cleared messages for MQ Mock: {}", mqMockExtId);
    }

    /**
     * Load MQ Mock by external ID and verify user access
     */
    MQMock loadById(final String externalId, final SmockinUser smockinUser) throws RecordNotFoundException {
        MQMock mqMock = mqMockDAO.findByExtIdAndUser(externalId, smockinUser.getId());

        if (mqMock == null) {
            throw new RecordNotFoundException();
        }

        return mqMock;
    }

    /**
     * Convert entity to DTO
     */
    MQMockDTO toMQMockDTO(final MQMock mqMock) {
        MQMockDTO dto = new MQMockDTO(
                mqMock.getExtId(),
                mqMock.getName(),
                mqMock.getMqType().name(),
                mqMock.getDestinationName(),
                mqMock.isTopic(),
                mqMock.getBrokerUrl(),
                mqMock.isSaveMessages(),
                mqMock.getStatus().name(),
                mqMock.getCreatedBy().getExtId()
        );

        dto.setProperties(mqMock.getProperties());
        dto.setMessageCount((int) mqMockMessageDAO.countByMqMockExtId(mqMock.getExtId()));

        return dto;
    }

    /**
     * Convert message entity to DTO
     */
    MQMockMessageDTO toMQMockMessageDTO(final MQMockMessage message) {
        Map<String, String> headersMap = Map.of();

        if (StringUtils.isNotBlank(message.getHeaders())) {
            try {
                headersMap = GeneralUtils.deserialiseJson(message.getHeaders(), new TypeReference<Map<String, String>>(){});
            } catch (Exception e) {
                logger.warn("Failed to parse headers for message: {}", message.getExtId());
            }
        }

        return new MQMockMessageDTO(
                message.getExtId(),
                message.getMessageKey(),
                message.getMessageBody(),
                message.getContentType(),
                headersMap,
                message.getDateReceived(),
                message.getProducerId(),
                message.getMqMock().getExtId()
        );
    }

    /**
     * Validate MQ Mock DTO
     */
    void validateMQMockDTO(final MQMockDTO dto) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("MQ Mock data is required");
        }

        if (StringUtils.isBlank(dto.getName())) {
            throw new ValidationException("MQ Mock name is required");
        }

        if (StringUtils.isBlank(dto.getMqType())) {
            throw new ValidationException("MQ type is required");
        }

        if (StringUtils.isBlank(dto.getDestinationName())) {
            throw new ValidationException("Destination name (Queue/Topic) is required");
        }

        if (dto.getDestinationName().length() > 200) {
            throw new ValidationException("Destination name must not exceed 200 characters");
        }
    }
}
