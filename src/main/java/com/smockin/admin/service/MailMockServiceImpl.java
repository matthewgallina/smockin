package com.smockin.admin.service;

import com.smockin.admin.dto.MailMockDTO;
import com.smockin.admin.dto.response.MailMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.MailMockDAO;
import com.smockin.admin.persistence.entity.MailMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MailMockServiceImpl implements MailMockService {

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private MailMockDAO mailMockDAO;


    public List<MailMockResponseDTO> loadAll(final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        return mailMockDAO.findAllByUser(smockinUser.getId())
                .stream()
                .map(m ->
                        new MailMockResponseDTO(
                                m.getExtId(),
                                m.getDateCreated(),
                                m.getAddress(),
                                m.getStatus()))
                .collect(Collectors.toList());
    }

    public MailMockResponseDTO loadById(final String externalId, final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        return new MailMockResponseDTO(
                mailMock.getExtId(),
                mailMock.getDateCreated(),
                mailMock.getAddress(),
                mailMock.getStatus());
    }

    public String create(final MailMockDTO mailMockDTO, final String token) throws RecordNotFoundException, ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = new MailMock(mailMockDTO.getAddress(), mailMockDTO.getStatus(), smockinUser);

        return mailMockDAO.save(mailMock).getExtId();
    }

    public void update(final String externalId, final MailMockDTO mailMockDTO, final String token) throws RecordNotFoundException, ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        mailMock.setAddress(mailMockDTO.getAddress());
        mailMock.setStatus(mailMockDTO.getStatus());

        mailMockDAO.save(mailMock);
    }

    public void delete(final String externalId, final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        mailMockDAO.delete(mailMock);
    }

    private MailMock loadById(final String extId, final SmockinUser smockinUser) throws RecordNotFoundException {

        final MailMock mailMock = mailMockDAO.findByExtIdAndUser(extId, smockinUser.getId());

        if (mailMock == null) {
            throw new RecordNotFoundException();
        }

        return mailMock;
    }

}
