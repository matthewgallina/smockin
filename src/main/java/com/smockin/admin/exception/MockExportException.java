package com.smockin.admin.exception;

public class MockExportException extends RuntimeException {

    public MockExportException(final String msg) {
        super(msg);
    }

    public MockExportException(final String msg, Throwable cause) {
        super(msg, cause);
    }

}
