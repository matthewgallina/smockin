package com.smockin.admin.service;

import com.smockin.admin.dto.CallAnalyticDTO;
import com.smockin.admin.dto.response.CallAnalyticLogResponseDTO;
import com.smockin.admin.dto.response.CallAnalyticsResponseDTO;
import com.smockin.admin.dto.response.CallAnalyticsResponseLiteDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.CallAnalyticDAO;
import com.smockin.admin.persistence.dao.CallAnalyticLogDAO;
import com.smockin.admin.persistence.entity.CallAnalytic;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.service.CallAnalyticsCaptureService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CallAnalyticServiceImpl implements CallAnalyticService {

    private CallAnalyticDAO callAnalyticDAO;
    private CallAnalyticLogDAO callAnalyticLogDAO;
    private UserTokenServiceUtils userTokenServiceUtils;
    private CallAnalyticsCaptureService callAnalyticsCaptureService;

    private final String NAME_REGEX = "^[a-zA-Z0-9._-]*$";

    @Autowired
    public CallAnalyticServiceImpl(final CallAnalyticDAO callAnalyticDAO,
                                   final CallAnalyticLogDAO callAnalyticLogDAO,
                                   final UserTokenServiceUtils userTokenServiceUtils,
                                   final CallAnalyticsCaptureService callAnalyticsCaptureService) {
        this.callAnalyticDAO = callAnalyticDAO;
        this.callAnalyticLogDAO = callAnalyticLogDAO;
        this.userTokenServiceUtils = userTokenServiceUtils;
        this.callAnalyticsCaptureService = callAnalyticsCaptureService;
    }

    public List<CallAnalyticsResponseLiteDTO> getAll(final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        return callAnalyticDAO.findAll(smockinUser.getId())
                .stream()
                .map(ca ->
                        new CallAnalyticsResponseLiteDTO(ca.getExtId(), callAnalyticLogDAO.countAllApiLogsForCallAnalyticId(ca.getId()),0 ,0, ca.getName()))
                .collect(Collectors.toList());
    }

    public List<String> getAllActiveIds() throws RecordNotFoundException {

        return callAnalyticDAO.findAllActive()
                .stream()
                .map(CallAnalytic::getExtId)
                .collect(Collectors.toList());

    }

    public CallAnalyticsResponseDTO getById(final String externalId,
                                            final String token) throws RecordNotFoundException, ValidationException {

        final CallAnalytic callAnalytic = callAnalyticDAO.findByExtId(externalId);

        if (callAnalytic == null) {
            throw new RecordNotFoundException();
        }

        userTokenServiceUtils.validateRecordOwner(callAnalytic.getCreatedBy(), token);

        return new CallAnalyticsResponseDTO(
                callAnalytic.getExtId(),
                callAnalytic.getName(),
                    callAnalytic.getLogs()
                    .stream()
                    .map(cal ->
                            new CallAnalyticLogResponseDTO(
                                    cal.getOriginType(),
                                    cal.getPath(),
                                    cal.getResult(),
                                    cal.getDateCreated()))
                    .collect(Collectors.toList()));
    }

    public String create(final CallAnalyticDTO dto,
                         final String token) throws ValidationException, RecordNotFoundException {

        if (StringUtils.isBlank(dto.getName())) {
            throw new ValidationException("Name is undefined");
        }
        if (!dto.getName().matches(NAME_REGEX)) {
            throw new ValidationException("Invalid Name. Only alphanumeric characters are supported.");
        }

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final CallAnalytic callAnalytic = new CallAnalytic();
        callAnalytic.setName(dto.getName());
        callAnalytic.setStatus(RecordStatusEnum.ACTIVE);
        callAnalytic.setCreatedBy(smockinUser);

        final String callAnalyticId = callAnalyticDAO.save(callAnalytic).getExtId();

        callAnalyticsCaptureService.register(callAnalytic.getExtId());

        return callAnalyticId;
    }

    public void update(final String externalId,
                       final CallAnalyticDTO dto,
                       final String token) throws RecordNotFoundException, ValidationException {

        if (StringUtils.isBlank(dto.getName())) {
            throw new ValidationException("Name is undefined");
        }
        if (!dto.getName().matches(NAME_REGEX)) {
            throw new ValidationException("Invalid Name. Only alphanumeric characters are supported.");
        }

        final CallAnalytic callAnalytic = callAnalyticDAO.findByExtId(externalId);

        if (callAnalytic == null) {
            throw new RecordNotFoundException();
        }

        userTokenServiceUtils.validateRecordOwner(callAnalytic.getCreatedBy(), token);

        callAnalytic.setName(dto.getName());
        callAnalytic.setStatus(RecordStatusEnum.ACTIVE);

        callAnalyticDAO.save(callAnalytic);
    }

    public void delete(final String externalId,
                       final String token) throws RecordNotFoundException, ValidationException {

        final CallAnalytic callAnalytic = callAnalyticDAO.findByExtId(externalId);

        if (callAnalytic == null) {
            throw new RecordNotFoundException();
        }

        userTokenServiceUtils.validateRecordOwner(callAnalytic.getCreatedBy(), token);

        final String extId = callAnalytic.getExtId();

        callAnalyticDAO.delete(callAnalytic);

        callAnalyticsCaptureService.remove(extId);
    }

}
