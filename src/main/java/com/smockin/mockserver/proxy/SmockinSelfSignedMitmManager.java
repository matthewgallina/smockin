package com.smockin.mockserver.proxy;

import io.netty.handler.codec.http.HttpRequest;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import org.littleshoot.proxy.MitmManager;

import java.io.File;

public class SmockinSelfSignedMitmManager implements MitmManager {

    private SmockinSelfSignedSslEngineSource selfSignedSslEngineSource;

    public SmockinSelfSignedMitmManager() {

        final File f = new File(System.getProperty("user.home") + "/.smockin/proxy");

        if (!f.exists()) {
            f.mkdir();
        }

        selfSignedSslEngineSource = new SmockinSelfSignedSslEngineSource( f.getAbsolutePath() + File.separator + "littleproxy_keystore.jks", true, true);
    }

    public SSLEngine serverSslEngine(String peerHost, int peerPort) {
        return this.selfSignedSslEngineSource.newSslEngine(peerHost, peerPort);
    }

    public SSLEngine serverSslEngine() {
        return this.selfSignedSslEngineSource.newSslEngine();
    }

    public SSLEngine clientSslEngineFor(HttpRequest httpRequest, SSLSession serverSslSession) {
        return this.selfSignedSslEngineSource.newSslEngine();
    }

}
