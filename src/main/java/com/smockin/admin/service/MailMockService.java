package com.smockin.admin.service;

import com.smockin.admin.dto.MailMockDTO;
import com.smockin.admin.dto.response.MailMockResponseDTO;
import com.smockin.admin.dto.response.MailMockResponseLiteDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.dto.MailServerMessageInboxDTO;

import java.util.List;

public interface MailMockService {

    List<MailMockResponseLiteDTO> loadAll(final String token) throws RecordNotFoundException;
    MailMockResponseDTO loadById(final String externalId, final String token) throws RecordNotFoundException;
    String create(final MailMockDTO mailMockDTO, final String token) throws RecordNotFoundException, ValidationException;
    void update(final String externalId, final MailMockDTO mailMockDTO, final String token) throws RecordNotFoundException, ValidationException;
    void delete(final String externalId, final String token) throws RecordNotFoundException;
    List<MailServerMessageInboxDTO> loadMessagesFromMailServerInbox(final String externalId, final String token) throws ValidationException;

}
