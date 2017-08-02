package com.smockin.mockserver.dto;

/**
 * Created by mgallina.
 */
public class MockServerState {

    private boolean running;
    private int port;

    public MockServerState() {
    }

    public MockServerState(boolean running, int port) {
        this.running = running;
        this.port = port;
    }

    public boolean isRunning() {
        return running;
    }
    public void setRunning(boolean running) {
        this.running = running;
    }

    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }

}
