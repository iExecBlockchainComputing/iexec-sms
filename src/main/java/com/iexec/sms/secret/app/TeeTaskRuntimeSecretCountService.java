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
     */
    public void setAppRuntimeSecretCount(String appAddress,
                                         OwnerRole secretOwnerRole,
                                         Integer secretCount) {
        appAddress = appAddress.toLowerCase();
        final TeeTaskRuntimeSecretCount teeTaskRuntimeSecretCount =
                TeeTaskRuntimeSecretCount.builder()
                        .appAddress(appAddress)
                        .secretOwnerRole(secretOwnerRole)
                        .secretCount(secretCount)
                        .build();
        log.info("Adding new app runtime secret count" +
                        "[ownerRole:{}, appAddress:{}, secretCount:{}]",
                OwnerRole.REQUESTER, appAddress, secretCount);
        teeTaskRuntimeSecretCountRepository.save(teeTaskRuntimeSecretCount);
    }

    /**
     * Retrieve a secret count.
     */
    public Optional<TeeTaskRuntimeSecretCount> getAppRuntimeSecretCount(String appAddress,
                                                                        OwnerRole secretOwnerRole) {
        appAddress = appAddress.toLowerCase();
        return teeTaskRuntimeSecretCountRepository.findByAppAddressAndOwnerRole(
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
                                                  OwnerRole secretOwnerRole) {
        return getAppRuntimeSecretCount(appAddress, secretOwnerRole).isPresent();
    }
}
