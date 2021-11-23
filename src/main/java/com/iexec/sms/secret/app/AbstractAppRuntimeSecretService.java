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

package com.iexec.sms.secret.app;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.AbstractSecretService;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public abstract class AbstractAppRuntimeSecretService extends AbstractSecretService {
    private final AppRuntimeSecretRepository appRuntimeSecretRepository;
    private final AppRuntimeSecretCountRepository appRuntimeSecretCountRepository;
    private final AppRuntimeSecretOwnerRole ownerRole;

    protected AbstractAppRuntimeSecretService(AppRuntimeSecretRepository appRuntimeSecretRepository,
                                              AppRuntimeSecretCountRepository appRuntimeSecretCountRepository,
                                              EncryptionService encryptionService,
                                              AppRuntimeSecretOwnerRole ownerRole) {
        super(encryptionService);
        this.appRuntimeSecretRepository = appRuntimeSecretRepository;
        this.appRuntimeSecretCountRepository = appRuntimeSecretCountRepository;
        this.ownerRole = ownerRole;
    }

    /**
     * Retrieve a secret identified by its app address and its index.
     * Decrypt it if required and if its decryption is required.
     */
    public Optional<AppRuntimeSecret> getSecret(String appAddress,
                                                long secretIndex,
                                                boolean shouldDecryptValue) {
        appAddress = appAddress.toLowerCase();
        Optional<AppRuntimeSecret> secret =
                appRuntimeSecretRepository.findByAddressAndIndexAndOwnerRole(
                        appAddress,
                        secretIndex,
                        ownerRole
                );
        if (secret.isEmpty()) {
            return Optional.empty();
        }
        if (shouldDecryptValue) {
            decryptSecret(secret.get());
        }
        return secret;
    }

    /**
     * Retrieve a secret identified by its app address and its index.
     * Do not decrypt it.
     */
    public Optional<AppRuntimeSecret> getSecret(String appAddress,
                                                long secretIndex) {
        return getSecret(appAddress, secretIndex, false);
    }

    /**
     * Check whether a secret exists.
     *
     * @return {@code true} if the secret exists in the database, {@code false} otherwise.
     */
    public boolean isSecretPresent(String appAddress, long secretIndex) {
        return getSecret(appAddress, secretIndex).isPresent();
    }

    /**
     * Stores encrypted secrets.
     */
    public void encryptAndSaveSecret(String appAddress, long secretIndex, String secretValue) {
        appAddress = appAddress.toLowerCase();
        AppRuntimeSecret appRuntimeSecret =
                new AppRuntimeSecret(appAddress, secretIndex, ownerRole, secretValue);
        encryptSecret(appRuntimeSecret);
        log.info("Adding new app runtime secret " +
                        "[appAddress:{}, secretIndex:{}, ownerRole:{}, encryptedSecretValue:{}]",
                appAddress, secretIndex, ownerRole, appRuntimeSecret.getValue());
        appRuntimeSecretRepository.save(appRuntimeSecret);
    }

    /**
     * Defines how many runtime secrets an app requires.
     */
    public void setAppRuntimeSecretCount(String appAddress,
                                         Integer secretCount) {
        appAddress = appAddress.toLowerCase();
        final AppRuntimeSecretCount appRuntimeSecretCount =
                AppRuntimeSecretCount.builder()
                        .ownerRole(ownerRole)
                        .appAddress(appAddress)
                        .secretCount(secretCount)
                        .build();
        log.info("Adding new app runtime secret count" +
                        "[ownerRole:{}, appAddress:{}, secretCount:{}]",
                AppRuntimeSecretOwnerRole.REQUESTER, appAddress, secretCount);
        appRuntimeSecretCountRepository.save(appRuntimeSecretCount);
    }

    /**
     * Retrieve a secret count identified by its app address.
     */
    public Optional<AppRuntimeSecretCount> getAppRuntimeSecretCount(String appAddress) {
        appAddress = appAddress.toLowerCase();
        return appRuntimeSecretCountRepository.findByAppAddressAndOwnerRole(
                appAddress,
                ownerRole);
    }

    /**
     * Check whether a secret count exists.
     *
     * @return {@code true} if the secret count exists in the database,
     * {@code false} otherwise.
     */
    public boolean isAppRuntimeSecretCountPresent(String appAddress) {
        return getAppRuntimeSecretCount(appAddress).isPresent();
    }
}
