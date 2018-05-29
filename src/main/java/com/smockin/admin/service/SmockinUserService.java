package com.smockin.admin.service;

import com.smockin.admin.dto.SmockinUserDTO;

import java.util.List;

/**
 * Created by gallina on 26/05/2018.
 */
public interface SmockinUserService {

    List<SmockinUserDTO> loadAllUsers();

}
