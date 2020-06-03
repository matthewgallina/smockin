package com.smockin.admin.service;

import com.smockin.admin.dto.UserKeyValueDataDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;

import java.util.List;

public interface UserKeyValueDataService {

    List<UserKeyValueDataDTO> loadAll(final String token) throws RecordNotFoundException;
    UserKeyValueDataDTO loadById(final String externalId, final String token) throws RecordNotFoundException, ValidationException;
    UserKeyValueDataDTO loadByKey(final String key, final long userId);
    void save(final List<UserKeyValueDataDTO> dtos, final String token) throws RecordNotFoundException, ValidationException;
    void update(final String externalId, final UserKeyValueDataDTO dto, final String token) throws RecordNotFoundException, ValidationException;
    void delete(final String externalId, final String token) throws RecordNotFoundException, ValidationException;

}
