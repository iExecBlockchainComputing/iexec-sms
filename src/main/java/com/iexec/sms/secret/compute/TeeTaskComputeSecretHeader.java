/*
 * Copyright 2022 IEXEC BLOCKCHAIN TECH
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

import jakarta.persistence.Embeddable;
import jakarta.validation.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

@Embeddable
@Slf4j
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeeTaskComputeSecretHeader implements Serializable {
    public static final int SECRET_KEY_MIN_LENGTH = 1;
    public static final int SECRET_KEY_MAX_LENGTH = 64;

    @NotNull
    private OnChainObjectType onChainObjectType;
    /**
     * Represents the blockchain address of the deployed object
     * (0xapplication, 0xdataset, 0xworkerpool)
     * as a lower case string.
     * <p>
     * In a future release, it should also handle ENS names.
     */
    @NotNull
    private String onChainObjectAddress; // Will be empty for a secret belonging to a requester
    @NotNull
    private SecretOwnerRole secretOwnerRole;
    @NotNull
    private String fixedSecretOwner; // Will be empty for a secret belonging to an application developer
    @NotNull
    @Size(min = SECRET_KEY_MIN_LENGTH, max = SECRET_KEY_MAX_LENGTH)
    private String key;

    public TeeTaskComputeSecretHeader(OnChainObjectType onChainObjectType,
                                      String onChainObjectAddress,
                                      SecretOwnerRole secretOwnerRole,
                                      String fixedSecretOwner,
                                      String key) {
        if (secretOwnerRole == SecretOwnerRole.REQUESTER && !StringUtils.isEmpty(onChainObjectAddress)) {
            throw new ValidationException("On-chain object address should be empty for a requester secret.");
        }

        if (secretOwnerRole == SecretOwnerRole.APPLICATION_DEVELOPER && !StringUtils.isEmpty(fixedSecretOwner)) {
            throw new ValidationException("Fixed secret owner should be empty for an application developer secret.");
        }

        this.onChainObjectAddress = onChainObjectAddress == null ? "" : onChainObjectAddress.toLowerCase();
        this.onChainObjectType = onChainObjectType;
        this.secretOwnerRole = secretOwnerRole;
        this.fixedSecretOwner = fixedSecretOwner == null ? "" : fixedSecretOwner.toLowerCase();
        this.key = key;

        validateFields();
    }

    private void validateFields() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            final Validator validator = factory.getValidator();
            final Set<ConstraintViolation<TeeTaskComputeSecretHeader>> issues = validator.validate(this);
            if (!issues.isEmpty()) {
                log.warn("{}", issues.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList()));
                throw new ValidationException("Can't create TeeTaskComputeSecretHeader.");
            }
        }
    }
}
