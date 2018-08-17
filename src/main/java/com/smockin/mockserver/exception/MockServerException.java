package com.smockin.mockserver.exception;

/**
 * Created by mgallina.
 */
public class MockServerException extends RuntimeException {

    public MockServerException(String msg) {
        super(msg);
    }

    public MockServerException(Throwable ex) {
        super(ex);
    }

    public MockServerException(String msg, Throwable ex) {
        super(msg, ex);
    }

}
