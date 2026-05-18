package com.smockin.admin.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.dto.MQMockDTO;
import com.smockin.mockserver.dto.MQMockMessageDTO;
import com.smockin.mockserver.dto.MockServerState;

import java.util.List;
import java.util.Optional;

public interface MQMockService {

    List<MQMockDTO> loadAll(final String token) throws RecordNotFoundException;

    MQMockDTO loadById(final String externalId, final String token) throws RecordNotFoundException;

    String create(final MQMockDTO mqMockDTO, final String token) throws RecordNotFoundException, ValidationException;

    void update(final String externalId, final MQMockDTO mqMockDTO, final String token) throws RecordNotFoundException, ValidationException;

    void delete(final String externalId, final String token) throws RecordNotFoundException;

    MockServerState getMQServerState() throws RecordNotFoundException;

    void startMQServer(final String token) throws RecordNotFoundException, ValidationException;

    void stopMQServer(final String token) throws RecordNotFoundException, ValidationException;

    List<MQMockMessageDTO> loadMessages(final String mqMockExtId, final String token) throws RecordNotFoundException;

    void sendMessage(final String mqMockExtId, final String messageBody, final String messageKey,
                     final String contentType, final String headers, final String token)
            throws RecordNotFoundException, ValidationException;

    void clearMessages(final String mqMockExtId, final String token) throws RecordNotFoundException;
}
