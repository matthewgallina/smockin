package com.smockin.admin.service;

import com.icegreen.greenmail.store.FolderException;
import com.smockin.admin.dto.MailMockDTO;
import com.smockin.admin.dto.response.MailMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.MailMockDAO;
import com.smockin.admin.persistence.entity.MailMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MailMessageDTO;
import com.smockin.mockserver.engine.MockedMailServerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MailMockServiceImpl implements MailMockService {

    private final Logger logger = LoggerFactory.getLogger(MailMockServiceImpl.class);

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private MailMockDAO mailMockDAO;

    @Autowired
    private MockedServerEngineService mockedServerEngineService;

    @Autowired
    private MockedMailServerEngine mockedMailServerEngine;


    public List<MailMockResponseDTO> loadAll(final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        return mailMockDAO.findAllByUser(smockinUser.getId())
                .stream()
                .map(m ->
                        new MailMockResponseDTO(
                                m.getExtId(),
                                m.getDateCreated(),
                                m.getAddress(),
                                m.getStatus(),
                                m.isSaveReceivedMail()))
                .collect(Collectors.toList());
    }

    public MailMockResponseDTO loadById(final String externalId, final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        return new MailMockResponseDTO(
                mailMock.getExtId(),
                mailMock.getDateCreated(),
                mailMock.getAddress(),
                mailMock.getStatus(),
                mailMock.isSaveReceivedMail());
    }

    public String create(final MailMockDTO mailMockDTO, final String token) throws RecordNotFoundException, ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = new MailMock(mailMockDTO.getAddress(), mailMockDTO.getStatus(), smockinUser, mailMockDTO.isSaveReceivedMail());

        final String extId = mailMockDAO.save(mailMock).getExtId();

        if (mockedServerEngineService.getMailServerState().isRunning()) {
            try {
                mockedMailServerEngine.addMailUser(mailMock);
            } catch (MessagingException | FolderException e) {
                logger.error("Error adding user ");
            }
        }

        return extId;
    }

    public void update(final String externalId, final MailMockDTO mailMockDTO, final String token) throws RecordNotFoundException, ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        mailMock.setAddress(mailMockDTO.getAddress());
        mailMock.setStatus(mailMockDTO.getStatus());
        mailMock.setSaveReceivedMail(mailMockDTO.isSaveReceivedMail());

        mailMockDAO.save(mailMock);

        // TODO update user's mail box from mail server
    }

    public void delete(final String externalId, final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        mailMockDAO.delete(mailMock);

        // TODO delete user's mail box from mail server

    }

    public List<MailMessageDTO> loadAllInboxAddressMessages(final String externalId, final String token)
            throws ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        if (!mockedServerEngineService.getMailServerState().isRunning()) {
            throw new ValidationException("Mail server is not running");
        }

        return mockedMailServerEngine.getAllEmailsForAddress(mailMock.getAddress());
    }

    private MailMock loadById(final String extId, final SmockinUser smockinUser) throws RecordNotFoundException {

        final MailMock mailMock = mailMockDAO.findByExtIdAndUser(extId, smockinUser.getId());

        if (mailMock == null) {
            throw new RecordNotFoundException();
        }

        return mailMock;
    }

}
