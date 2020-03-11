package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.mockserver.service.StatefulService;
import com.smockin.utils.GeneralUtils;
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
public class HttpStatefulController {

    @Autowired
    private StatefulService statefulService;

    @RequestMapping(path="/stateful/{extId}/clear", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> clearDataState(@PathVariable("extId") final String extId,
                                                        @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException {

        statefulService.resetState(extId, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

}
