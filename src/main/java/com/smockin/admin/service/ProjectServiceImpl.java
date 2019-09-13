package com.smockin.admin.service;

import com.smockin.admin.dto.ProjectDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulProjectDAO;
import com.smockin.admin.persistence.entity.RestfulProject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    @Autowired
    private RestfulProjectDAO restfulProjectDAO;

    @Override
    public List<ProjectDTO> loadAll(final String token) throws RecordNotFoundException {

        return restfulProjectDAO.findAll()
                .stream().map(p -> new ProjectDTO(p.getExtId(), p.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String create(final ProjectDTO projectDTO, final String token) throws RecordNotFoundException, ValidationException {

        // Validation
        if (StringUtils.isBlank(projectDTO.getName())) {
            throw new ValidationException("Project 'name' is required");
        }

        // Create
        final RestfulProject project = new RestfulProject();
        project.setName(projectDTO.getName());

        return restfulProjectDAO.save(project).getExtId();
    }

    @Override
    public void update(final String extId, final ProjectDTO projectDTO, final String token) throws RecordNotFoundException, ValidationException {

        // Validation
        if (!StringUtils.equals(extId, projectDTO.getExtId())) {
            throw new ValidationException("Invalid Identifier");
        }
        if (StringUtils.isBlank(projectDTO.getName())) {
            throw new ValidationException("Project 'name' is required");
        }

        // Update
        final RestfulProject project = loadByExtId(extId);
        project.setName(projectDTO.getName());

        restfulProjectDAO.save(project);
    }

    @Override
    public void delete(final String extId, final String token) throws RecordNotFoundException {

        restfulProjectDAO.delete(loadByExtId(extId));
    }

    @Override
    public RestfulProject loadByExtId(final String extId) {

        final RestfulProject project = restfulProjectDAO.findByExtId(extId);

        if (project == null) {
            throw new RecordNotFoundException();
        }

        return project;
    }

}
