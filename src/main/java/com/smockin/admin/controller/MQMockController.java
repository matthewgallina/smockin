package com.smockin.admin.controller;

import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.MQMockService;
import com.smockin.mockserver.dto.MQMockDTO;
import com.smockin.mockserver.dto.MQMockMessageDTO;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class MQMockController {

    @Autowired
    private MQMockService mqMockService;

    /**
     * Get all MQ Mocks for current user
     */
    @RequestMapping(path="/mqmock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<MQMockDTO>> getAll(
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {
        return ResponseEntity.ok(mqMockService.loadAll(GeneralUtils.extractOAuthToken(bearerToken)));
    }

    /**
     * Get MQ Mock by external ID
     */
    @RequestMapping(path="/mqmock/{extId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<MQMockDTO> get(
            @PathVariable("extId") final String extId,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {
        return ResponseEntity.ok(mqMockService.loadById(extId, GeneralUtils.extractOAuthToken(bearerToken)));
    }

    /**
     * Create a new MQ Mock
     */
    @RequestMapping(path="/mqmock", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO<String>> create(
            @RequestBody final MQMockDTO dto,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {
        return new ResponseEntity<>(
                new SimpleMessageResponseDTO<>(mqMockService.create(dto, GeneralUtils.extractOAuthToken(bearerToken))),
                HttpStatus.CREATED);
    }

    /**
     * Update an existing MQ Mock
     */
    @RequestMapping(path="/mqmock/{extId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> update(
            @PathVariable("extId") final String extId,
            @RequestBody final MQMockDTO dto,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {
        mqMockService.update(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete an MQ Mock
     */
    @RequestMapping(path="/mqmock/{extId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> delete(
            @PathVariable("extId") final String extId,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {
        mqMockService.delete(extId, GeneralUtils.extractOAuthToken(bearerToken));
        return ResponseEntity.noContent().build();
    }

    /**
     * Get MQ Server state
     */
    @RequestMapping(path="/mqmock/server/state", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<MockServerState> getServerState(
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) {
        return ResponseEntity.ok(mqMockService.getMQServerState());
    }

    /**
     * Start MQ Server
     */
    @RequestMapping(path="/mqmock/server/start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> startServer(
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {
        mqMockService.startMQServer(GeneralUtils.extractOAuthToken(bearerToken));
        return ResponseEntity.ok().build();
    }

    /**
     * Stop MQ Server
     */
    @RequestMapping(path="/mqmock/server/stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> stopServer(
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {
        mqMockService.stopMQServer(GeneralUtils.extractOAuthToken(bearerToken));
        return ResponseEntity.ok().build();
    }

    /**
     * Get messages for a MQ Mock
     */
    @RequestMapping(path="/mqmock/{extId}/messages", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<MQMockMessageDTO>> getMessages(
            @PathVariable("extId") final String extId,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {
        return ResponseEntity.ok(mqMockService.loadMessages(extId, GeneralUtils.extractOAuthToken(bearerToken)));
    }

    /**
     * Clear messages for a MQ Mock
     */
    @RequestMapping(path="/mqmock/{extId}/messages", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> clearMessages(
            @PathVariable("extId") final String extId,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {
        mqMockService.clearMessages(extId, GeneralUtils.extractOAuthToken(bearerToken));
        return ResponseEntity.noContent().build();
    }

    /**
     * Send message to MQ Mock
     */
    @RequestMapping(path="/mqmock/{extId}/message/send", method = RequestMethod.POST, 
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> sendMessage(
            @PathVariable("extId") final String extId,
            @RequestBody final MQSendMessageRequest request,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {
        
        mqMockService.sendMessage(
                extId,
                request.getMessageBody(),
                request.getMessageKey(),
                request.getContentType(),
                request.getHeaders(),
                GeneralUtils.extractOAuthToken(bearerToken));
        
        return ResponseEntity.ok().build();
    }

    /**
     * Request DTO for sending messages
     */
    public static class MQSendMessageRequest {
        private String messageBody;
        private String messageKey;
        private String contentType;
        private String headers;

        public String getMessageBody() { return messageBody; }
        public void setMessageBody(String messageBody) { this.messageBody = messageBody; }
        public String getMessageKey() { return messageKey; }
        public void setMessageKey(String messageKey) { this.messageKey = messageKey; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public String getHeaders() { return headers; }
        public void setHeaders(String headers) { this.headers = headers; }
    }
}
