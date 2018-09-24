package com.smockin.mockserver.proxy;

import com.google.common.io.ByteStreams;
import org.littleshoot.proxy.SslEngineSource;
import org.littleshoot.proxy.extras.SelfSignedSslEngineSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class SmockinSelfSignedSslEngineSource implements SslEngineSource {
    private static final Logger LOG = LoggerFactory.getLogger(SelfSignedSslEngineSource.class);
    private static final String ALIAS = "littleproxy";
    private static final String PASSWORD = "Be Your Own Lantern";
    private static final String PROTOCOL = "TLS";
    private final File keyStoreFile;
    private final boolean trustAllServers;
    private final boolean sendCerts;
    private SSLContext sslContext;

    public SmockinSelfSignedSslEngineSource(String keyStorePath, boolean trustAllServers, boolean sendCerts) {
        this.trustAllServers = trustAllServers;
        this.sendCerts = sendCerts;
        this.keyStoreFile = new File(keyStorePath);
        this.initializeKeyStore();
        this.initializeSSLContext();
    }

    public SmockinSelfSignedSslEngineSource(String keyStorePath) {
        this(keyStorePath, false, true);
    }

    public SmockinSelfSignedSslEngineSource(boolean trustAllServers) {
        this(trustAllServers, true);
    }

    public SmockinSelfSignedSslEngineSource(boolean trustAllServers, boolean sendCerts) {
        this("littleproxy_keystore.jks", trustAllServers, sendCerts);
    }

    public SmockinSelfSignedSslEngineSource() {
        this(false);
    }

    public SSLEngine newSslEngine() {
        return this.sslContext.createSSLEngine();
    }

    public SSLEngine newSslEngine(String peerHost, int peerPort) {
        return this.sslContext.createSSLEngine(peerHost, peerPort);
    }

    public SSLContext getSslContext() {
        return this.sslContext;
    }

    private void initializeKeyStore() {
        if (this.keyStoreFile.isFile()) {
            LOG.info("Not deleting keystore");
        } else {
            final String dir = this.keyStoreFile.getParent() + File.separator;
            this.nativeCall("keytool", "-genkey", "-alias", "littleproxy", "-keysize", "4096", "-validity", "36500", "-keyalg", "RSA", "-dname", "CN=littleproxy", "-keypass", "Be Your Own Lantern", "-storepass", "Be Your Own Lantern", "-keystore", dir + this.keyStoreFile.getName());
            this.nativeCall("keytool", "-exportcert", "-alias", "littleproxy", "-keystore", dir + this.keyStoreFile.getName(), "-storepass", "Be Your Own Lantern", "-file", dir + "littleproxy_cert");
        }
    }

    private void initializeSSLContext() {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }

        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(this.keyStoreFile), "Be Your Own Lantern".toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, "Be Your Own Lantern".toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
            tmf.init(ks);
            TrustManager[] trustManagers = null;
            if (!this.trustAllServers) {
                trustManagers = tmf.getTrustManagers();
            } else {
                trustManagers = new TrustManager[]{new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    }

                    public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }};
            }

            KeyManager[] keyManagers = null;
            if (this.sendCerts) {
                keyManagers = kmf.getKeyManagers();
            } else {
                keyManagers = new KeyManager[0];
            }

            this.sslContext = SSLContext.getInstance("TLS");
            this.sslContext.init(keyManagers, trustManagers, (SecureRandom)null);
        } catch (Exception var7) {
            throw new Error("Failed to initialize the server-side SSLContext", var7);
        }
    }

    private String nativeCall(String... commands) {
        LOG.info("Running '{}'", Arrays.asList(commands));
        ProcessBuilder pb = new ProcessBuilder(commands);

        try {
            Process process = pb.start();
            InputStream is = process.getInputStream();
            byte[] data = ByteStreams.toByteArray(is);
            String dataAsString = new String(data);
            LOG.info("Completed native call: '{}'\nResponse: '" + dataAsString + "'", Arrays.asList(commands));
            return dataAsString;
        } catch (IOException var7) {
            LOG.error("Error running commands: " + Arrays.asList(commands), var7);
            return "";
        }
    }
}
