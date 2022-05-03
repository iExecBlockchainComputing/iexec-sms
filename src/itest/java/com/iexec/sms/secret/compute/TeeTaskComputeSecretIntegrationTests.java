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

package com.iexec.sms.secret.compute;

import com.iexec.common.contract.generated.Ownable;
import com.iexec.common.utils.HashUtils;
import com.iexec.sms.CommonTestSetup;
import com.iexec.sms.api.SmsClient;
import com.iexec.sms.api.SmsClientBuilder;
import com.iexec.sms.api.SmsService;
import com.iexec.sms.encryption.EncryptionService;
import feign.FeignException;
import feign.Logger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.http.HttpStatus;
import org.web3j.crypto.Hash;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.iexec.common.utils.SignatureUtils.signMessageHashAndGetSignature;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TeeTaskComputeSecretIntegrationTests extends CommonTestSetup {
    private static final String APP_ADDRESS = "0xabcd1339ec7e762e639f4887e2bfe5ee8023e23e";
    private static final String UPPER_CASE_APP_ADDRESS = "0xABCD1339EC7E762E639F4887E2BFE5EE8023E23E";
    private static final String SECRET_VALUE = "secretValue";
    private static final String OWNER_ADDRESS = "0xabcd1339ec7e762e639f4887e2bfe5ee8023e23e";
    private static final String REQUESTER_ADDRESS = "0x123790ae4E14865B972ee04a5f9FD5fB153Cd5e7";
    private static final String DOMAIN = "IEXEC_SMS_DOMAIN";
    private static final String APP_DEVELOPER_PRIVATE_KEY = "0x2fac4d263f1b20bfc33ea2bcb1cbe1521322dbde81d04b0c454ffff1218f0ed6";
    private static final String REQUESTER_PRIVATE_KEY = "0xb8e97e9e217a50dedbe3c0c4c37b85a85a10d4eb23fca6dbad55162cfbb1c450";

    private SmsService smsService;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private TeeTaskComputeSecretRepository repository;

    @BeforeEach
    private void setUp() {
        SmsClient smsClient = SmsClientBuilder.getInstance(Logger.Level.FULL, "http://localhost:" + randomServerPort);
        smsService = new SmsService(smsClient);
        final Ownable appContract = mock(Ownable.class);
        when(appContract.getContractAddress()).thenReturn(APP_ADDRESS);
        when(iexecHubService.getOwnableContract(APP_ADDRESS))
                .thenReturn(appContract);
        repository.deleteAll();
    }

    @Test
    void shouldAddNewComputeSecrets() {
        String authorization;
        final String appDeveloperSecretIndex = "0";
        final String requesterSecretKey="secret-key";
        final int requesterSecretCount = 1;
        final String requesterAddress = REQUESTER_ADDRESS;
        final String appAddress = APP_ADDRESS;
        final String secretValue = SECRET_VALUE;
        final String ownerAddress = OWNER_ADDRESS;

        addNewAppDeveloperSecret(appAddress, appDeveloperSecretIndex, secretValue, ownerAddress);
        setRequesterSecretCount(appAddress, requesterSecretCount, ownerAddress);
        addNewRequesterSecret(requesterAddress, requesterSecretKey, secretValue);

        // Check the new secrets exists for the API
        assertThat(smsService.isAppDeveloperAppComputeSecretPresent(appAddress, appDeveloperSecretIndex))
                .isTrue();

        assertThat(smsService.isRequesterAppComputeSecretPresent(requesterAddress, requesterSecretKey))
                .isTrue();

        // We check the secrets have been added to the database
        final ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withIgnorePaths("value");
        final Optional<TeeTaskComputeSecret> appDeveloperSecret = repository.findOne(
                Example.of(TeeTaskComputeSecret
                                .builder()
                                .onChainObjectType(OnChainObjectType.APPLICATION)
                                .onChainObjectAddress(appAddress)
                                .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                                .key(appDeveloperSecretIndex)
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
        assertThat(appDeveloperSecret.get().getId()).isNotBlank();
        assertThat(appDeveloperSecret.get().getOnChainObjectAddress()).isEqualToIgnoringCase(appAddress);
        assertThat(appDeveloperSecret.get().getKey()).isEqualTo(appDeveloperSecretIndex);
        assertThat(appDeveloperSecret.get().getValue()).isNotEqualTo(secretValue);
        assertThat(appDeveloperSecret.get().getValue()).isEqualTo(encryptionService.encrypt(secretValue));

        final Optional<TeeTaskComputeSecret> requesterSecret = repository.findOne(
                Example.of(TeeTaskComputeSecret
                                .builder()
                                .onChainObjectType(OnChainObjectType.APPLICATION)
                                .onChainObjectAddress("")
                                .secretOwnerRole(SecretOwnerRole.REQUESTER)
                                .fixedSecretOwner(requesterAddress)
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
        assertThat(requesterSecret.get().getId()).isNotBlank();
        assertThat(requesterSecret.get().getOnChainObjectAddress()).isEqualToIgnoringCase("");
        assertThat(requesterSecret.get().getKey()).isEqualTo(requesterSecretKey);
        assertThat(requesterSecret.get().getValue()).isNotEqualTo(secretValue);
        assertThat(requesterSecret.get().getValue()).isEqualTo(encryptionService.encrypt(secretValue));

        // We shouldn't be able to add secrets to the database with the same IDs
        authorization = getAuthorizationForAppDeveloper(appAddress, appDeveloperSecretIndex, secretValue);
        assertThat(smsService.addAppDeveloperAppComputeSecret(
                authorization, appAddress, secretValue))
                .isFalse();
        authorization = getAuthorizationForRequester(requesterAddress, requesterSecretKey, secretValue);
        assertThat(smsService.addRequesterAppComputeSecret(
                authorization, requesterAddress, requesterSecretKey, secretValue))
                .isFalse();

        // We shouldn't be able to add a new secret to the database with the same index
        // and an appAddress whose only difference is the case.
        when(iexecHubService.getOwner(UPPER_CASE_APP_ADDRESS)).thenReturn(ownerAddress);
        authorization = getAuthorizationForAppDeveloper(UPPER_CASE_APP_ADDRESS, appDeveloperSecretIndex, secretValue);
        assertThat(smsService.addAppDeveloperAppComputeSecret(
                authorization, UPPER_CASE_APP_ADDRESS, secretValue)).isFalse();
    }

    @Test
    void addMultipleRequesterSecrets() {
        List<String> keys = List.of("secret-key-1", "secret-key-2", "secret-key-3");
        for (String key : keys) {
            addNewRequesterSecret(REQUESTER_ADDRESS, key, SECRET_VALUE);
        }
        assertThat(repository.count()).isEqualTo(keys.size());
        List<TeeTaskComputeSecret> secrets = repository.findAll();
        assertThat(secrets.stream().map(TeeTaskComputeSecret::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("secret-key-1", "secret-key-2", "secret-key-3");

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "this-is-a-really-long-key-with-far-too-many-characters-in-its-name",
            "this-is-a-key-with-invalid-characters:!*~"
    })
    void checkInvalidRequesterSecretKey(String secretKey) {
        final String authorization = getAuthorizationForRequester(REQUESTER_ADDRESS, secretKey, SECRET_VALUE);
        assertThat(smsService.addRequesterAppComputeSecret(authorization, REQUESTER_ADDRESS, secretKey, SECRET_VALUE)).isFalse();
        assertThat(repository.count()).isZero();
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
        assertThat(smsService.isAppDeveloperAppComputeSecretPresent(appAddress, secretIndex)).isFalse();
        // Add a new secret to the database
        assertThat(smsService.addAppDeveloperAppComputeSecret(authorization, appAddress, secretValue)).isTrue();
    }

    private void setRequesterSecretCount(String appAddress, int secretCount, String ownerAddress) {
        when(iexecHubService.getOwner(appAddress)).thenReturn(ownerAddress);
        final String authorization = getAuthorizationForRequesterSecretCount(appAddress, secretCount);
        assertThat(smsService.setMaxRequesterSecretCountForAppCompute(authorization, appAddress, secretCount)).isTrue();
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
        assertThat(smsService.isRequesterAppComputeSecretPresent(requesterAddress, secretKey)).isFalse();
        // Add a new secret to the database
        assertThat(smsService.addRequesterAppComputeSecret(authorization, requesterAddress, secretKey, secretValue)).isTrue();
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
     * a requester secret count to database.
     */
    private String getAuthorizationForRequesterSecretCount(
            String appAddress,
            int secretCount) {
        final String challenge = HashUtils.concatenateAndHash(
                Hash.sha3String(DOMAIN),
                appAddress,
                Long.toHexString(secretCount));
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
