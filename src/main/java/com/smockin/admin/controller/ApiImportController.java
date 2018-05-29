package com.smockin.admin.controller;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.exception.ApiImportException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.ApiImportRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Created by mgallina.
 */
@Controller
public class ApiImportController {

    @Autowired
    private ApiImportRouter apiImportRouter;

    @RequestMapping(path="/api/import", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<Void> create(@RequestBody final ApiImportDTO dto) throws ApiImportException, ValidationException {

        apiImportRouter.route(dto);

        return new ResponseEntity<Void>(HttpStatus.CREATED);
    }

}
