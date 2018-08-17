package com.smockin.admin.service;

public interface EncryptionService {

    String encrypt(final String passwordPlain);
    boolean verify(final String passwordPlain, final String passwordEnc);

}
