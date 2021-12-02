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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class TeeTaskComputeSecretCountService {
    protected final TeeTaskComputeSecretCountRepository teeTaskComputeSecretCountRepository;

    public TeeTaskComputeSecretCountService(TeeTaskComputeSecretCountRepository teeTaskComputeSecretCountRepository) {
        this.teeTaskComputeSecretCountRepository = teeTaskComputeSecretCountRepository;
    }

    /**
     * Defines how many Compute secrets an app can handle.
     *
     * @return {@code true} if {@code maxSecretCount} is positive
     * and has been correctly inserted in DB, {@code false} otherwise
     */
    public boolean setMaxAppComputeSecretCount(String appAddress,
                                               SecretOwnerRole secretOwnerRole,
                                               Integer maxSecretCount) {
        if (maxSecretCount < 0) {
            return false;
        }
        appAddress = appAddress.toLowerCase();
        final TeeTaskComputeSecretCount teeTaskComputeSecretCount =
                TeeTaskComputeSecretCount.builder()
                        .appAddress(appAddress)
                        .secretOwnerRole(secretOwnerRole)
                        .secretCount(maxSecretCount)
                        .build();
        log.info("Adding new app compute secret count" +
                        " [ownerRole:{}, appAddress:{}, maxSecretCount:{}]",
                SecretOwnerRole.REQUESTER, appAddress, maxSecretCount);
        teeTaskComputeSecretCountRepository.save(teeTaskComputeSecretCount);

        return true;
    }

    /**
     * Retrieve a max secret count.
     */
    public Optional<TeeTaskComputeSecretCount> getMaxAppComputeSecretCount(String appAddress,
                                                                           SecretOwnerRole secretOwnerRole) {
        appAddress = appAddress.toLowerCase();
        return teeTaskComputeSecretCountRepository.findByAppAddressAndSecretOwnerRole(
                appAddress,
                secretOwnerRole);
    }

    /**
     * Check whether a max secret count exists.
     *
     * @return {@code true} if the secret count exists in the database,
     * {@code false} otherwise.
     */
    public boolean isMaxAppComputeSecretCountPresent(String appAddress,
                                                     SecretOwnerRole secretOwnerRole) {
        return getMaxAppComputeSecretCount(appAddress, secretOwnerRole).isPresent();
    }
}
