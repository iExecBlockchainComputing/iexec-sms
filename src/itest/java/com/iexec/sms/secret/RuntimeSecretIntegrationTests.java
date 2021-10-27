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

import com.iexec.sms.ApiClient;
import com.iexec.sms.CommonTestSetup;
import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.runtime.RuntimeSecret;
import com.iexec.sms.secret.runtime.RuntimeSecretRepository;
import feign.FeignException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.mockito.Mockito.when;

public class RuntimeSecretIntegrationTests extends CommonTestSetup {
    private static final String APP_ADDRESS   = "appAddress";
    private static final String SECRET_VALUE  = "secretValue";
    private static final String AUTHORIZATION = "authorization";
    private static final String CHALLENGE     = "challenge";

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private RuntimeSecretRepository repository;

    @Test
    void shouldAddNewRuntimeSecret() {
        when(authorizationService.getChallengeForSetRuntimeSecret(APP_ADDRESS, 0, SECRET_VALUE)).thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS)).thenReturn(true);

        // At first, no secret should be in the database
        final Optional<RuntimeSecret> noSecret = repository.findByAddressIgnoreCaseAndIndex(APP_ADDRESS, 0);
        Assertions.assertThat(noSecret).isEmpty();

        // We add a new secret to the database
        apiClient.addRuntimeSecret(AUTHORIZATION, APP_ADDRESS, 0, SECRET_VALUE);

        // We check the secret has been added to the database
        final Optional<RuntimeSecret> secret = repository.findByAddressIgnoreCaseAndIndex(APP_ADDRESS, 0);
        if (secret.isEmpty()) {
            // Could be something like `Assertions.assertThat(secret).isPresent()`
            // but Sonar needs a call to `secret.isEmpty()` to avoid triggering a warning.
            Assertions.fail("A secret was expected but none has been retrieved.");
            return;
        }
        Assertions.assertThat(secret.get().getId()).isNotBlank();
        Assertions.assertThat(secret.get().getAddress()).isEqualToIgnoringCase(APP_ADDRESS);
        Assertions.assertThat(secret.get().getIndex()).isZero();
        Assertions.assertThat(secret.get().getValue()).isNotEqualTo(SECRET_VALUE);
        Assertions.assertThat(secret.get().getValue()).isEqualTo(encryptionService.encrypt(SECRET_VALUE));
        Assertions.assertThat(secret.get().isEncryptedValue()).isEqualTo(true);

        // We shouldn't be able to add a new secret to the database with the same appAddress/index
        try {
            apiClient.addRuntimeSecret(AUTHORIZATION, APP_ADDRESS, 0, SECRET_VALUE);
            Assertions.fail("A second runtime secret with the same app address and index should be rejected.");
        } catch (FeignException.Conflict ignored) {
            // Having a Conflict exception is what we expect there.
        }
    }
}
