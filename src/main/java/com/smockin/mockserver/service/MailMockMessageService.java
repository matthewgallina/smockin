package com.smockin.mockserver.service;

import java.util.Date;

public interface MailMockMessageService {

    void saveMessage(final String mailMockExtId,
                     final String sender,
                     final String subject,
                     final String body,
                     final Date dateReceived);

}
