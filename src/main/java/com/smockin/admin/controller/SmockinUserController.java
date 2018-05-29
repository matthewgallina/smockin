package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.service.SmockinUserService;
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
public class SmockinUserController {

    @Autowired
    private SmockinUserService smockinUserService;

    @RequestMapping(path="/user", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<Void> getClients() throws RecordNotFoundException {

        return new ResponseEntity<Void>(HttpStatus.OK);
    }



}
