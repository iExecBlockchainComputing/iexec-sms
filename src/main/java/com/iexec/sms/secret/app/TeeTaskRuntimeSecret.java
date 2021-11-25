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

import com.iexec.sms.secret.Secret;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
public class TeeTaskRuntimeSecret extends Secret {
    private DeployedObjectType deployedObjectType;
    private OwnerRole secretOwnerRole;
    private String fixedOwner;  // May be null if the owner is not fixed
    private long index;

    public TeeTaskRuntimeSecret(
            DeployedObjectType deployedObjectType,
            String deployedObjectAddress,
            OwnerRole secretOwnerRole,
            String fixedOwner,
            long index,
            String value) {
        super(deployedObjectAddress, value);
        this.deployedObjectType = deployedObjectType;
        this.secretOwnerRole = secretOwnerRole;
        this.fixedOwner = fixedOwner;
        this.index = index;
    }
}
