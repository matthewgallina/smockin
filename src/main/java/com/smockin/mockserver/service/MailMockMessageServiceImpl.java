package com.smockin.mockserver.service;

import com.smockin.admin.persistence.dao.MailMockDAO;
import com.smockin.admin.persistence.entity.MailMock;
import com.smockin.admin.persistence.entity.MailMockMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
public class MailMockMessageServiceImpl implements MailMockMessageService {

    private final Logger logger = LoggerFactory.getLogger(MailMockMessageServiceImpl.class);

    @Autowired
    private MailMockDAO mailMockDAO;

    @Override
    public void saveMessage(final String mailMockExtId,
                            final String sender,
                            final String subject,
                            final String body,
                            final Date dateReceived) {

        logger.debug("saveMessage called");

        final MailMock mailMock = mailMockDAO.findByExtId(mailMockExtId);

        if (mailMock == null) {
            logger.error("Error locating mail mock with external ID: " + mailMockExtId);
            return;
        }

        final MailMockMessage mailMockMessage = new MailMockMessage();
        mailMockMessage.setFrom(sender);
        mailMockMessage.setDateReceived(dateReceived);
        mailMockMessage.setSubject(subject);
        mailMockMessage.setBody(body);
        mailMockMessage.setMailMock(mailMock);

        mailMock.getMessages().add(mailMockMessage);

        mailMockDAO.save(mailMock);
    }

}
