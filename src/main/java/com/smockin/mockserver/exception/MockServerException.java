package com.smockin.mockserver.exception;

/**
 * Created by mgallina.
 */
public class MockServerException extends Exception {

    public MockServerException(String msg) {
        super(msg);
    }

    public MockServerException(Throwable ex) {
        super(ex);
    }

}
