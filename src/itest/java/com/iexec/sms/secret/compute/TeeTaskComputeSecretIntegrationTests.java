/*
 * Copyright 2021-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.compute;

import com.iexec.commons.poco.utils.HashUtils;
import com.iexec.sms.CommonTestSetup;
import com.iexec.sms.api.SmsClient;
import com.iexec.sms.api.SmsClientBuilder;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.encryption.EncryptionService;
import feign.FeignException;
import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.web3j.crypto.Hash;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static com.iexec.commons.poco.utils.SignatureUtils.signMessageHashAndGetSignature;
import static com.iexec.sms.MockChainConfiguration.MOCK_CHAIN_PROFILE;
import static org.mockito.Mockito.when;

@Slf4j
@ActiveProfiles({"scone", MOCK_CHAIN_PROFILE, "test"})
class TeeTaskComputeSecretIntegrationTests extends CommonTestSetup {
    private static final String APP_ADDRESS = "0xabcd1339ec7e762e639f4887e2bfe5ee8023e23e";
    private static final String UPPER_CASE_APP_ADDRESS = "0xABCD1339EC7E762E639F4887E2BFE5EE8023E23E";
    private static final String SECRET_VALUE = generateRandomAscii();
    private static final String OWNER_ADDRESS = "0xabcd1339ec7e762e639f4887e2bfe5ee8023e23e";
    private static final String REQUESTER_ADDRESS = "0x123790ae4E14865B972ee04a5f9FD5fB153Cd5e7";
    private static final String DOMAIN = "IEXEC_SMS_DOMAIN";
    private static final String APP_DEVELOPER_PRIVATE_KEY = "0x2fac4d263f1b20bfc33ea2bcb1cbe1521322dbde81d04b0c454ffff1218f0ed6";
    private static final String REQUESTER_PRIVATE_KEY = "0xb8e97e9e217a50dedbe3c0c4c37b85a85a10d4eb23fca6dbad55162cfbb1c450";

    private SmsClient apiClient;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private TeeTaskComputeSecretRepository repository;

    @Autowired
    private IexecHubService iexecHubService;

    /**
     * Generate random ASCII from seed for re-testability.
     * See also {@link org.apache.commons.lang3.RandomStringUtils#randomAscii(int)}
     */
    private static String generateRandomAscii() {
        long seed = new Date().getTime();
        log.info("Generating random ascii from seed: {}", seed);
        return RandomStringUtils.random(
                4096,
                32,
                127,
                false,
                false,
                null,
                new Random(seed));
    }

    @BeforeEach
    public void setUp() {
        apiClient = SmsClientBuilder.getInstance(Logger.Level.FULL, "http://localhost:" + randomServerPort);
        repository.deleteAll();
    }

    @Test
    void shouldAddNewComputeSecrets() {
        final String requesterSecretKey = "secret-key";
        final String requesterAddress = REQUESTER_ADDRESS;
        final String appAddress = APP_ADDRESS;
        final String secretValue = SECRET_VALUE;

        addNewAppDeveloperSecret(appAddress, SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue, OWNER_ADDRESS);
        addNewRequesterSecret(requesterAddress, requesterSecretKey, secretValue);

        // Check the new secrets exists for the API
        try {
            apiClient.isAppDeveloperAppComputeSecretPresent(appAddress);
        } catch (FeignException e) {
            Assertions.assertThat(e.status()).isEqualTo(HttpStatus.NO_CONTENT.value());
        }

        try {
            apiClient.isRequesterAppComputeSecretPresent(requesterAddress, requesterSecretKey);
        } catch (FeignException e) {
            Assertions.assertThat(e.status()).isEqualTo(HttpStatus.NO_CONTENT.value());
        }

        // We check the secrets have been added to the database
        final ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withIgnorePaths("value");
        final Optional<TeeTaskComputeSecret> appDeveloperSecret = repository.findOne(
                Example.of(TeeTaskComputeSecret
                                .builder()
                                .onChainObjectType(OnChainObjectType.APPLICATION)
                                .onChainObjectAddress(appAddress)
                                .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                                .key(SmsClient.APP_DEVELOPER_SECRET_INDEX)
                                .build(),
                        exampleMatcher
                )
        );
        if (appDeveloperSecret.isEmpty()) {
            // Could be something like `Assertions.assertThat(appDeveloperSecret).isPresent()`
            // but Sonar needs a call to `appDeveloperSecret.isEmpty()` to avoid triggering a warning.
            Assertions.fail("An app developer secret was expected but none has been retrieved.");
            return;
        }
        Assertions.assertThat(appDeveloperSecret.get().getHeader().getOnChainObjectAddress()).isEqualToIgnoringCase(appAddress);
        Assertions.assertThat(appDeveloperSecret.get().getHeader().getKey()).isEqualTo(SmsClient.APP_DEVELOPER_SECRET_INDEX);
        Assertions.assertThat(appDeveloperSecret.get().getValue()).isNotEqualTo(secretValue);
        Assertions.assertThat(appDeveloperSecret.get().getValue()).isEqualTo(encryptionService.encrypt(secretValue));

        final Optional<TeeTaskComputeSecret> requesterSecret = repository.findOne(
                Example.of(TeeTaskComputeSecret
                                .builder()
                                .onChainObjectType(OnChainObjectType.APPLICATION)
                                .onChainObjectAddress("")
                                .secretOwnerRole(SecretOwnerRole.REQUESTER)
                                .fixedSecretOwner(requesterAddress.toLowerCase())
                                .key(requesterSecretKey)
                                .build(),
                        exampleMatcher)
        );
        if (requesterSecret.isEmpty()) {
            // Could be something like `Assertions.assertThat(requesterSecret).isPresent()`
            // but Sonar needs a call to `requesterSecret.isEmpty()` to avoid triggering a warning.
            Assertions.fail("An app requester secret was expected but none has been retrieved.");
            return;
        }
        Assertions.assertThat(requesterSecret.get().getHeader().getOnChainObjectAddress()).isEqualToIgnoringCase("");
        Assertions.assertThat(requesterSecret.get().getHeader().getKey()).isEqualTo(requesterSecretKey);
        Assertions.assertThat(requesterSecret.get().getValue()).isNotEqualTo(secretValue);
        Assertions.assertThat(requesterSecret.get().getValue()).isEqualTo(encryptionService.encrypt(secretValue));

        // We shouldn't be able to add a new secrets to the database with the same IDs
        try {
            final String authorization = getAuthorizationForAppDeveloper(appAddress, SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue);
            apiClient.addAppDeveloperAppComputeSecret(authorization, appAddress, secretValue);
            Assertions.fail("A second app developer secret with the same app address and index should be rejected.");
        } catch (FeignException.Conflict ignored) {
            // Having a Conflict exception is what we expect there.
        }
        try {
            final String authorization = getAuthorizationForRequester(requesterAddress, requesterSecretKey, secretValue);
            apiClient.addRequesterAppComputeSecret(authorization, requesterAddress, requesterSecretKey, secretValue);
            Assertions.fail("A second app requester secret with the same app address and index should be rejected.");
        } catch (FeignException.Conflict ignored) {
            // Having a Conflict exception is what we expect there.
        }

        // We shouldn't be able to add a new secret to the database with the same index
        // and an appAddress whose only difference is the case.
        try {
            final String authorization = getAuthorizationForAppDeveloper(UPPER_CASE_APP_ADDRESS, SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue);
            apiClient.addAppDeveloperAppComputeSecret(authorization, UPPER_CASE_APP_ADDRESS, secretValue);
            Assertions.fail("A second app developer secret with the same index " +
                    "and an app address whose only difference is the case should be rejected.");
        } catch (FeignException.Conflict ignored) {
            // Having a Conflict exception is what we expect there.
        }
    }

    @Test
    void addMultipleRequesterSecrets() {
        List<String> keys = List.of("secret-key-1", "secret-key-2", "secret-key-3");
        for (String key : keys) {
            addNewRequesterSecret(REQUESTER_ADDRESS, key, SECRET_VALUE);
        }
        Assertions.assertThat(repository.count()).isEqualTo(keys.size());
        List<TeeTaskComputeSecret> secrets = repository.findAll();
        final List<String> retrievedKeys = secrets
                .stream()
                .map(TeeTaskComputeSecret::getHeader)
                .map(TeeTaskComputeSecretHeader::getKey)
                .toList();
        Assertions.assertThat(retrievedKeys)
                .containsExactlyInAnyOrder("secret-key-1", "secret-key-2", "secret-key-3");

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "this-is-a-really-long-key-with-far-too-many-characters-in-its-name",
            "this-is-a-key-with-invalid-characters:!*~"
    })
    void checkInvalidRequesterSecretKey(String secretKey) {
        Assertions.assertThatThrownBy(() -> addNewRequesterSecret(REQUESTER_ADDRESS, secretKey, SECRET_VALUE))
                .isInstanceOf(FeignException.BadRequest.class);
        Assertions.assertThat(repository.count()).isZero();
    }

    /**
     * Checks no application developer secret already exists with given appAddress/index couple
     * and adds a new application developer secret to the database
     */
    @SuppressWarnings("SameParameterValue")
    private void addNewAppDeveloperSecret(String appAddress, String secretIndex, String secretValue, String ownerAddress) {
        when(iexecHubService.getOwner(appAddress)).thenReturn(ownerAddress);

        final String authorization = getAuthorizationForAppDeveloper(appAddress, secretIndex, secretValue);

        // At first, no secret should be in the database
        try {
            apiClient.isAppDeveloperAppComputeSecretPresent(appAddress);
            Assertions.fail("No application developer secret was expected but one has been retrieved.");
        } catch (FeignException.NotFound ignored) {
            // Having a Not Found exception is what we expect there.
        }

        // Add a new secret to the database
        try {
            apiClient.addAppDeveloperAppComputeSecret(authorization, appAddress, secretValue);
        } catch (FeignException e) {
            Assertions.assertThat(e.status()).isEqualTo(HttpStatus.NO_CONTENT.value());
        }
    }

    /**
     * Checks no requester secret already exists with given appAddress/index couple
     * and adds a new requester secret to the database
     */
    @SuppressWarnings("SameParameterValue")
    private void addNewRequesterSecret(String requesterAddress,
                                       String secretKey,
                                       String secretValue) {
        final String authorization = getAuthorizationForRequester(requesterAddress, secretKey, secretValue);

        // At first, no secret should be in the database
        try {
            apiClient.isRequesterAppComputeSecretPresent(requesterAddress, secretKey);
            Assertions.fail("No application requester secret was expected but one has been retrieved.");
        } catch (FeignException.NotFound ignored) {
            // Having a Not Found exception is what we expect there.
        }

        // Add a new secret to the database
        try {
            apiClient.addRequesterAppComputeSecret(authorization, requesterAddress, secretKey, secretValue);
        } catch (FeignException e) {
            Assertions.assertThat(e.status()).isEqualTo(HttpStatus.NO_CONTENT.value());
        }
    }

    /**
     * Forges an authorization that'll permit adding
     * given application developer secret to database.
     */
    private String getAuthorizationForAppDeveloper(
            String appAddress,
            String secretIndex,
            String secretValue) {
        final String challenge = HashUtils.concatenateAndHash(
                Hash.sha3String(DOMAIN),
                appAddress,
                Hash.sha3String(secretIndex),
                Hash.sha3String(secretValue));
        return signMessageHashAndGetSignature(challenge, APP_DEVELOPER_PRIVATE_KEY).getValue();
    }

    /**
     * Forges an authorization that'll permit adding
     * given requester secret to database.
     */
    private String getAuthorizationForRequester(
            String requesterAddress,
            String secretKey,
            String secretValue) {

        final String challenge = HashUtils.concatenateAndHash(
                Hash.sha3String(DOMAIN),
                requesterAddress,
                Hash.sha3String(secretKey),
                Hash.sha3String(secretValue));
        return signMessageHashAndGetSignature(challenge, REQUESTER_PRIVATE_KEY).getValue();
    }
}
