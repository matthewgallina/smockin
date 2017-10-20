package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.exception.MockServerException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by mgallina.
 */
@ControllerAdvice
public class ExceptionHandlerController {

    @ResponseStatus(value=HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public void badRequest() {

    }

    @ResponseStatus(value=HttpStatus.NOT_FOUND)
    @ExceptionHandler(RecordNotFoundException.class)
    public void notFound() {

    }

    @ResponseStatus(value=HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    public String ValidationBadRequest(ValidationException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(MockServerException.class)
    public void internalServerError(MockServerException ex) {

    }
    
}
