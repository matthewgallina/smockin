package com.smockin.admin.service;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import com.smockin.admin.exception.ValidationException;

/**
 * Created by mgallina.
 */
public interface HttpClientService {

    String HTTP_PROTOCOL = "http://";
    String HTTPS_PROTOCOL = "https://";

    HttpClientResponseDTO handleExternalCall(final HttpClientCallDTO httpClientCallDTO) throws ValidationException;
    HttpClientResponseDTO handleCallToMock(final HttpClientCallDTO httpClientCallDTO) throws ValidationException;

}
