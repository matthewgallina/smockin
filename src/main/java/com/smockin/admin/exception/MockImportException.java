package com.smockin.admin.exception;

public class MockImportException extends RuntimeException {

    public MockImportException(final String msg) {
        super(msg);
    }

    public MockImportException(final String msg, Throwable cause) {
        super(msg, cause);
    }

}
