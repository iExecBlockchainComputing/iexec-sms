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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class TeeTaskRuntimeSecretCountService {
    protected final TeeTaskRuntimeSecretCountRepository teeTaskRuntimeSecretCountRepository;

    public TeeTaskRuntimeSecretCountService(TeeTaskRuntimeSecretCountRepository teeTaskRuntimeSecretCountRepository) {
        this.teeTaskRuntimeSecretCountRepository = teeTaskRuntimeSecretCountRepository;
    }

    /**
     * Defines how many runtime secrets an app requires.
     *
     * @return {@code true} if {@code secretCount} is positive
     * and has been correctly inserted in DB, {@code false} otherwise
     */
    public boolean setAppRuntimeSecretCount(String appAddress,
                                         SecretOwnerRole secretOwnerRole,
                                         Integer secretCount) {
        if (secretCount < 0) {
            return false;
        }
        appAddress = appAddress.toLowerCase();
        final TeeTaskRuntimeSecretCount teeTaskRuntimeSecretCount =
                TeeTaskRuntimeSecretCount.builder()
                        .appAddress(appAddress)
                        .secretOwnerRole(secretOwnerRole)
                        .secretCount(secretCount)
                        .build();
        log.info("Adding new app runtime secret count" +
                        "[ownerRole:{}, appAddress:{}, secretCount:{}]",
                SecretOwnerRole.REQUESTER, appAddress, secretCount);
        teeTaskRuntimeSecretCountRepository.save(teeTaskRuntimeSecretCount);

        return true;
    }

    /**
     * Retrieve a secret count.
     */
    public Optional<TeeTaskRuntimeSecretCount> getAppRuntimeSecretCount(String appAddress,
                                                                        SecretOwnerRole secretOwnerRole) {
        appAddress = appAddress.toLowerCase();
        return teeTaskRuntimeSecretCountRepository.findByAppAddressAndSecretOwnerRole(
                appAddress,
                secretOwnerRole);
    }

    /**
     * Check whether a secret count exists.
     *
     * @return {@code true} if the secret count exists in the database,
     * {@code false} otherwise.
     */
    public boolean isAppRuntimeSecretCountPresent(String appAddress,
                                                  SecretOwnerRole secretOwnerRole) {
        return getAppRuntimeSecretCount(appAddress, secretOwnerRole).isPresent();
    }
}
