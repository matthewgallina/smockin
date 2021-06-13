package com.smockin.admin.service;

import com.smockin.admin.dto.UserKeyValueDataDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.UserKeyValueDataDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.entity.UserKeyValueData;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserKeyValueDataServiceImpl implements UserKeyValueDataService {

    @Autowired
    private UserKeyValueDataDAO userKeyValueDataDAO;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;


    @Override
    public List<UserKeyValueDataDTO> loadAll(final String token) throws RecordNotFoundException {

        final SmockinUser user = userTokenServiceUtils.loadCurrentActiveUser(token);

        return userKeyValueDataDAO
                .findAllByUser(user.getId())
                .stream()
                .map(kvp -> new UserKeyValueDataDTO(kvp.getExtId(), kvp.getKey(), kvp.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public UserKeyValueDataDTO loadById(String externalId, String token) throws RecordNotFoundException, ValidationException {

        final UserKeyValueData userKeyValueData = userKeyValueDataDAO.findByExtId(externalId);

        if (userKeyValueData == null) {
            throw new RecordNotFoundException();
        }

        userTokenServiceUtils.validateRecordOwner(userKeyValueData.getCreatedBy(), token);

        return new UserKeyValueDataDTO(userKeyValueData.getExtId(),
                userKeyValueData.getKey(),
                userKeyValueData.getValue());
    }

    @Override
    public UserKeyValueDataDTO loadByKey(final String key, final long userId) {

        final UserKeyValueData userKeyValueData = userKeyValueDataDAO.findByKey(key, userId);

        if (userKeyValueData == null) {
            return null;
        }

        return new UserKeyValueDataDTO(
                userKeyValueData.getExtId(),
                userKeyValueData.getKey(),
                userKeyValueData.getValue());
    }

    @Override
    public void save(final List<UserKeyValueDataDTO> dtos, final String token) throws RecordNotFoundException, ValidationException {

        final SmockinUser user = userTokenServiceUtils.loadCurrentActiveUser(token);

        dtos.stream().forEach(dto -> {

            final UserKeyValueData userKeyValueData = new UserKeyValueData();
            userKeyValueData.setKey(dto.getKey());
            userKeyValueData.setValue(dto.getValue());
            userKeyValueData.setCreatedBy(user);

            userKeyValueDataDAO.save(userKeyValueData);
        });
    }

    @Override
    public void update(final String externalId, final UserKeyValueDataDTO dto, final String token) throws RecordNotFoundException, ValidationException {

        final UserKeyValueData userKeyValueData = userKeyValueDataDAO.findByExtId(externalId);

        if (userKeyValueData == null) {
            throw new RecordNotFoundException();
        }

        userTokenServiceUtils.validateRecordOwner(userKeyValueData.getCreatedBy(), token);

        userKeyValueData.setKey(dto.getKey());
        userKeyValueData.setValue(dto.getValue());

        userKeyValueDataDAO.save(userKeyValueData);
    }

    @Override
    public void delete(final String externalId, final String token) throws RecordNotFoundException, ValidationException {

        final UserKeyValueData userKeyValueData = userKeyValueDataDAO.findByExtId(externalId);

        if (userKeyValueData == null) {
            throw new RecordNotFoundException();
        }

        userTokenServiceUtils.validateRecordOwner(userKeyValueData.getCreatedBy(), token);

        userKeyValueDataDAO.delete(userKeyValueData);
    }

}
