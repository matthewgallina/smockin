package com.smockin.admin.service;

import com.smockin.admin.dto.ProjectDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.entity.RestfulProject;
import java.util.List;

public interface ProjectService {

    String create(final ProjectDTO projectDTO, final String token) throws RecordNotFoundException, ValidationException;
    void update(final String extId, final ProjectDTO projectDTO, final String token) throws RecordNotFoundException, ValidationException;
    void delete(final String extId, final String token) throws RecordNotFoundException;
    List<ProjectDTO> loadAll(final String token) throws RecordNotFoundException;
    RestfulProject loadByExtId(final String extId);

}
