/*
 * Copyright 2021 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.sms.secret;

import com.iexec.common.utils.HashUtils;
import com.iexec.sms.ApiClient;
import com.iexec.sms.CommonTestSetup;
import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.app.ApplicationRuntimeSecret;
import com.iexec.sms.secret.app.ApplicationRuntimeSecretRepository;
import feign.FeignException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.web3j.crypto.Hash;

import java.util.Optional;

import static com.iexec.common.utils.SignatureUtils.signMessageHashAndGetSignature;
import static org.mockito.Mockito.when;

public class ApplicationRuntimeSecretIntegrationTests extends CommonTestSetup {
    private static final String APP_ADDRESS   = "0xabcd1339ec7e762e639f4887e2bfe5ee8023e23e";
    private static final String UPPER_CASE_APP_ADDRESS   = "0XABCD1339EC7E762E639F4887E2BFE5EE8023E23E";
    private static final String SECRET_VALUE  = "secretValue";
    private static final String OWNER_ADDRESS = "0xabcd1339ec7e762e639f4887e2bfe5ee8023e23e";
    private static final String DOMAIN        = "IEXEC_SMS_DOMAIN";
    private static final String PRIVATE_KEY   = "0x2fac4d263f1b20bfc33ea2bcb1cbe1521322dbde81d04b0c454ffff1218f0ed6";

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private ApplicationRuntimeSecretRepository repository;

    @Test
    void shouldAddNewRuntimeSecret() {
        final long secretIndex = 0;
        final String appAddress = APP_ADDRESS;
        final String secretValue = SECRET_VALUE;
        //noinspection UnnecessaryLocalVariable
        final String ownerAddress = OWNER_ADDRESS;

        addNewSecret(appAddress, secretIndex, secretValue, ownerAddress);

        // Check the new secret exists for the API
        ResponseEntity<Void> secretExistence = apiClient.isApplicationRuntimeSecretPresent(appAddress, secretIndex);
        Assertions.assertThat(secretExistence.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // We check the secret has been added to the database
        final Optional<ApplicationRuntimeSecret> secret = repository.findByAddressAndIndex(appAddress, secretIndex);
        if (secret.isEmpty()) {
            // Could be something like `Assertions.assertThat(secret).isPresent()`
            // but Sonar needs a call to `secret.isEmpty()` to avoid triggering a warning.
            Assertions.fail("A secret was expected but none has been retrieved.");
            return;
        }
        Assertions.assertThat(secret.get().getId()).isNotBlank();
        Assertions.assertThat(secret.get().getAddress()).isEqualToIgnoringCase(appAddress);
        Assertions.assertThat(secret.get().getIndex()).isZero();
        Assertions.assertThat(secret.get().getValue()).isNotEqualTo(secretValue);
        Assertions.assertThat(secret.get().getValue()).isEqualTo(encryptionService.encrypt(secretValue));
        Assertions.assertThat(secret.get().isEncryptedValue()).isEqualTo(true);

        // We shouldn't be able to add a new secret to the database with the same appAddress/index
        try {
            final String authorization = getAuthorization(appAddress, secretIndex, secretValue);
            apiClient.addApplicationRuntimeSecret(authorization, appAddress, secretIndex, secretValue);
            Assertions.fail("A second runtime secret with the same app address and index should be rejected.");
        } catch (FeignException.Conflict ignored) {
            // Having a Conflict exception is what we expect there.
        }

        // We shouldn't be able to add a new secret to the database with the same index
        // and an appAddress whose only difference is the case.
        try {
            when(iexecHubService.getOwner(UPPER_CASE_APP_ADDRESS)).thenReturn(ownerAddress);

            final String authorization = getAuthorization(UPPER_CASE_APP_ADDRESS, secretIndex, secretValue);
            apiClient.addApplicationRuntimeSecret(authorization, UPPER_CASE_APP_ADDRESS, secretIndex, secretValue);
            Assertions.fail("A second runtime secret with the same index " +
                    "and an app address whose only difference is the case should be rejected.");
        } catch (FeignException.Conflict ignored) {
            // Having a Conflict exception is what we expect there.
        }
    }

    /**
     * Checks no secret already exists with given appAddress/index couple
     * and adds a new runtime secret to the database
     */
    @SuppressWarnings("SameParameterValue")
    private void addNewSecret(String appAddress, long secretIndex, String secretValue, String ownerAddress) {
        when(iexecHubService.getOwner(appAddress)).thenReturn(ownerAddress);

        final String authorization = getAuthorization(appAddress, secretIndex, secretValue);

        // At first, no secret should be in the database
        try {
            apiClient.isApplicationRuntimeSecretPresent(appAddress, secretIndex);
            Assertions.fail("No secret was expected but one has been retrieved.");
        } catch (FeignException.NotFound ignored) {
            // Having a Not Found exception is what we expect there.
        }

        // Add a new secret to the database
        final ResponseEntity<String> secretCreationResult = apiClient.addApplicationRuntimeSecret(authorization, appAddress, secretIndex, secretValue);
        Assertions.assertThat(secretCreationResult.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    /**
     * Forges an authorization that'll permit adding given secret to database.
     */
    private String getAuthorization(String appAddress, long secretIndex, String secretValue) {
        final String challenge = HashUtils.concatenateAndHash(
                Hash.sha3String(DOMAIN),
                appAddress,
                Long.toHexString(secretIndex),
                Hash.sha3String(secretValue));
        return signMessageHashAndGetSignature(challenge, PRIVATE_KEY).getValue();
    }
}
