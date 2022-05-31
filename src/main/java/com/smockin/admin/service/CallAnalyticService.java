package com.smockin.admin.service;

import com.smockin.admin.dto.CallAnalyticDTO;
import com.smockin.admin.dto.response.CallAnalyticsResponseDTO;
import com.smockin.admin.dto.response.CallAnalyticsResponseLiteDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;

import java.util.List;

public interface CallAnalyticService {

    List<CallAnalyticsResponseLiteDTO> getAll(final String token) throws RecordNotFoundException;

    List<String> getAllActiveIds() throws RecordNotFoundException;

    CallAnalyticsResponseDTO getById(final String externalId,
                                     final String token) throws RecordNotFoundException, ValidationException;

    String create(final CallAnalyticDTO dto,
                  final String token) throws ValidationException, AuthException;

    void update(final String externalId,
                final CallAnalyticDTO dto,
                final String token) throws RecordNotFoundException, ValidationException;

    void delete(final String externalId,
                final String token) throws RecordNotFoundException, ValidationException;

}
