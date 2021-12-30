package com.smockin.admin.exception;

public class FileUploadException extends RuntimeException {

    public FileUploadException(final String msg) {
        super(msg);
    }

    public FileUploadException(final String msg, Throwable cause) {
        super(msg, cause);
    }

}
