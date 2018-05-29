package com.smockin.admin.service;

import com.smockin.admin.dto.SmockinUserDTO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by gallina on 26/05/2018.
 */
@Service
@Transactional
public class SmockinUserServiceImpl implements SmockinUserService {

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    @Override
    public List<SmockinUserDTO> loadAllUsers() {

        // TODO Only allow admin user to access this!

        return smockinUserDAO
                .findAll()
                .stream()
                .map(u -> new SmockinUserDTO(u.getExtId(), u.getUsername(), u.getFullName(), u.getCtxPath(), u.getRole()))
                .collect(Collectors.toList());

    }



}
