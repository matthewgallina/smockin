package com.smockin.mockserver.engine;

import com.smockin.mockserver.dto.MailMessageSearchDTO;
import com.smockin.mockserver.dto.MailServerMessageInboxAttachmentDTO;
import com.smockin.mockserver.dto.MailServerMessageInboxDTO;
import com.smockin.utils.GeneralUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MailInboxCache {

    private ConcurrentHashMap<CachedMailServerMessageKey, CachedMailServerMessage> mailMessageCache
            = new ConcurrentHashMap<>();

    public void clearAll() {
        mailMessageCache.clear();
    }

    public void add(final String mailMockExtId,
                    final CachedMailServerMessage mailServerMessageInbox) {

        mailMessageCache.put(
                new CachedMailServerMessageKey(
                        mailMockExtId,
                        mailServerMessageInbox.getMailServerMessageInboxDTO().getCacheID(),
                        GeneralUtils.getCurrentDate()), mailServerMessageInbox);
    }

    public void delete(final String mailMockExtId, final String messageId) {

        for (Map.Entry<CachedMailServerMessageKey, CachedMailServerMessage> item : mailMessageCache.entrySet()) {
            if (item.getKey() != null
                    && item.getKey().getMockMailId().equals(mailMockExtId)
                    && item.getKey().getMailMessageId().equals(messageId)) {
                mailMessageCache.remove(item.getKey());
                break;
            }
        }
    }

    public void deleteAll(final String mailMockExtId) {

        for (Map.Entry<CachedMailServerMessageKey, CachedMailServerMessage> item : mailMessageCache.entrySet()) {
            if (item.getKey() != null
                    && item.getKey().getMockMailId().equals(mailMockExtId)) {
                mailMessageCache.remove(item.getKey());
            }
        }
    }

    public List<CachedMailServerMessage> findAllMessages(final String mailMockExtId,
                                                         final Optional<MailMessageSearchDTO> mailMessageSearchDTO,
                                                         final Optional<Integer> pageStart) {

        // Sort by date and find page start...
        int start = (pageStart.isPresent())
                ? pageStart.get()
                : 0;

        final int startFromRecord = (start * GeneralUtils.DEFAULT_RECORDS_PER_PAGE);

        // Just supporting 'subject' in search for now...
        if (mailMessageSearchDTO.isPresent()
                && StringUtils.isNotBlank(mailMessageSearchDTO.get().getSubject())) {
            return mailMessageCache
                    .entrySet()
                    .stream()
                    .filter(e ->
                            e.getKey().getMockMailId().equals(mailMockExtId)
                                && StringUtils.contains(
                                    e.getValue().getMailServerMessageInboxDTO().getSubject(),
                                    mailMessageSearchDTO.get().getSubject()))
                    .sorted(Comparator.comparing(e ->
                            e.getKey().getDateAdded()))
                    .map(Map.Entry::getValue)
                    .skip(startFromRecord)
                    .limit(GeneralUtils.DEFAULT_RECORDS_PER_PAGE)
                    .collect(Collectors.toList());
        }

        return mailMessageCache
                .entrySet()
                .stream()
                .filter(e ->
                        e.getKey().getMockMailId().equals(mailMockExtId))
                .sorted(Comparator.comparing(e ->
                        e.getKey().getDateAdded()))
                .map(Map.Entry::getValue)
                .skip(startFromRecord)
                .limit(GeneralUtils.DEFAULT_RECORDS_PER_PAGE)
                .collect(Collectors.toList());
    }

    public long countAllMessages(final String mailMockExtId,
                                 final Optional<MailMessageSearchDTO> mailMessageSearchDTO) {

        if (mailMessageSearchDTO.isPresent()
                && StringUtils.isNotBlank(mailMessageSearchDTO.get().getSubject())) {
            return mailMessageCache
                    .entrySet()
                    .stream()
                    .filter(e ->
                            e.getKey().getMockMailId().equals(mailMockExtId)
                                && StringUtils.contains(
                                    e.getValue().getMailServerMessageInboxDTO().getSubject(),
                                    mailMessageSearchDTO.get().getSubject()))
                    .count();
        }

        return mailMessageCache
                .entrySet()
                .stream()
                .filter(e ->
                        e.getKey().getMockMailId().equals(mailMockExtId))
                .count();
    }

    public Optional<CachedMailServerMessage> findMessageById(final String mailMockExtId, final String messageId) {

        return mailMessageCache
                .entrySet()
                .stream()
                .filter(e ->
                        e.getKey().getMockMailId().equals(mailMockExtId)
                            && e.getKey().getMailMessageId().equals(messageId))
                .map(Map.Entry::getValue)
                .findFirst();
    }

}

@Data
@AllArgsConstructor
class CachedMailServerMessageKey {
    private String mockMailId;
    private String mailMessageId;
    private Date dateAdded;
}

@Data
@AllArgsConstructor
class CachedMailServerMessage {
    private MailServerMessageInboxDTO mailServerMessageInboxDTO;
    private List<MailServerMessageInboxAttachmentDTO> attachments;
}
