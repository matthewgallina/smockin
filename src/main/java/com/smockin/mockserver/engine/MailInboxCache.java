package com.smockin.mockserver.engine;

import com.smockin.mockserver.dto.MailServerMessageInboxAttachmentDTO;
import com.smockin.mockserver.dto.MailServerMessageInboxDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                        mailServerMessageInbox.getMailServerMessageInboxDTO().getCacheID()), mailServerMessageInbox);
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

    public List<CachedMailServerMessage> findAllMessages(final String mailMockExtId) {

        return mailMessageCache
                .entrySet()
                .stream()
                .filter(e ->
                        e.getKey().getMockMailId().equals(mailMockExtId))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
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
}

@Data
@AllArgsConstructor
class CachedMailServerMessage {
    private MailServerMessageInboxDTO mailServerMessageInboxDTO;
    private List<MailServerMessageInboxAttachmentDTO> attachments;
}
