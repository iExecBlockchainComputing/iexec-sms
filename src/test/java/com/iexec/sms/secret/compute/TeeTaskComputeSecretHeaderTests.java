/*
 * Copyright 2022-2024 IEXEC BLOCKCHAIN TECH
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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.validation.ValidationException;

class TeeTaskComputeSecretHeaderTests {
    private static final String ON_CHAIN_OBJECT_ADDRESS = "onChainObjectAddress";
    private static final String FIXED_SECRET_OWNER = "fixedSecretOwner";
    private static final String KEY = "key";

    // region Valid constructions
    @ParameterizedTest
    @EnumSource(OnChainObjectType.class)
    void shouldConstructNewApplicationDeveloperSecretHeader(OnChainObjectType objectType) {
        Assertions.assertThatNoException().isThrownBy(() -> new TeeTaskComputeSecretHeader(
                objectType,
                ON_CHAIN_OBJECT_ADDRESS,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                KEY
        ));
    }

    @ParameterizedTest
    @EnumSource(OnChainObjectType.class)
    void shouldConstructNewRequesterSecretHeader(OnChainObjectType objectType) {
        Assertions.assertThatNoException().isThrownBy(() -> new TeeTaskComputeSecretHeader(
                objectType,
                "",
                SecretOwnerRole.REQUESTER,
                FIXED_SECRET_OWNER,
                KEY
        ));
    }
    // endregion

    // region Invalid construction
    @Test
    void shouldNotConstructNewSecretHeaderBecauseOnChainObjectTypeIsNull() {
        Assertions.assertThatThrownBy(() -> new TeeTaskComputeSecretHeader(
                null,
                ON_CHAIN_OBJECT_ADDRESS,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                KEY
        )).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @EnumSource(OnChainObjectType.class)
    void shouldNotConstructSecretHeaderBecauseSecretOwnerRoleIsNull(OnChainObjectType objectType) {
        Assertions.assertThatThrownBy(() -> new TeeTaskComputeSecretHeader(
                objectType,
                ON_CHAIN_OBJECT_ADDRESS,
                null,
                FIXED_SECRET_OWNER,
                KEY
        )).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @EnumSource(OnChainObjectType.class)
    void shouldNotConstructSecretHeaderBecauseKeyIsNull(OnChainObjectType objectType) {
        Assertions.assertThatThrownBy(() -> new TeeTaskComputeSecretHeader(
                objectType,
                ON_CHAIN_OBJECT_ADDRESS,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                null
        )).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @EnumSource(OnChainObjectType.class)
    void shouldNotConstructNewApplicationDeveloperSecretHeaderBecauseFixedSecretOwnerNotEmpty(OnChainObjectType objectType) {
        Assertions.assertThatThrownBy(() -> new TeeTaskComputeSecretHeader(
                objectType,
                ON_CHAIN_OBJECT_ADDRESS,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                FIXED_SECRET_OWNER,
                KEY
        )).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @EnumSource(OnChainObjectType.class)
    void shouldNotConstructNewRequesterSecretHeaderBecauseOnChainObjectAddressNotEmpty(OnChainObjectType objectType) {
        Assertions.assertThatThrownBy(() -> new TeeTaskComputeSecretHeader(
                objectType,
                ON_CHAIN_OBJECT_ADDRESS,
                SecretOwnerRole.REQUESTER,
                "",
                KEY
        )).isInstanceOf(ValidationException.class);
    }
    // endregion
}
