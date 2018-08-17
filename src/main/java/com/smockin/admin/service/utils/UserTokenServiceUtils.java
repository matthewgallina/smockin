package com.smockin.admin.service.utils;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.entity.*;
import com.smockin.admin.service.SmockinUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by mgallina.
 */
@Component
@Transactional
public class UserTokenServiceUtils {

    @Autowired
    private SmockinUserService smockinUserService;

    public SmockinUser loadCurrentUser(final String token) throws RecordNotFoundException {

        if (UserModeEnum.INACTIVE.equals(smockinUserService.getUserMode())) {
            return smockinUserService.loadDefaultUser()
                    .orElseThrow(() -> new RecordNotFoundException());
        }

        return smockinUserService.loadCurrentUser(token);
    }

    public void validateRecordOwner(final SmockinUser recordOwner, final String token) throws RecordNotFoundException, ValidationException {

        final SmockinUser currentUser = loadCurrentUser(token);

        if (recordOwner.getId() != currentUser.getId()) {
            throw new ValidationException("Insufficient record access");
        }

    }

}
