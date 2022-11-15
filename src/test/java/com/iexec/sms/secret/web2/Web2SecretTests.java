package com.iexec.sms.secret.web2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Web2SecretTests {
    private static final String ID = "id";
    private static final String OWNER_ADDRESS = "ownerAddress";
    private static final String SECRET_ADDRESS = "secretAddress";

    private static final String UNENCRYPTED_VALUE = "unencrypted value";
    private static final String ENCRYPTED_VALUE   = "encrypted value";
    private static final Web2Secret UNENCRYPTED_SECRET = new Web2Secret(ID, OWNER_ADDRESS, SECRET_ADDRESS, UNENCRYPTED_VALUE, false);
    private static final Web2Secret ENCRYPTED_SECRET   = new Web2Secret(ID, OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_VALUE, true);

    @Test
    void withEncryptedValue() {
        assertEquals(ENCRYPTED_SECRET, UNENCRYPTED_SECRET.withEncryptedValue(ENCRYPTED_VALUE));
        assertEquals(ENCRYPTED_SECRET, ENCRYPTED_SECRET.withEncryptedValue(ENCRYPTED_VALUE));
    }

    @Test
    void withDecryptedValue() {
        assertEquals(UNENCRYPTED_SECRET, ENCRYPTED_SECRET.withDecryptedValue(UNENCRYPTED_VALUE));
        assertEquals(UNENCRYPTED_SECRET, UNENCRYPTED_SECRET.withDecryptedValue(UNENCRYPTED_VALUE));
    }
}