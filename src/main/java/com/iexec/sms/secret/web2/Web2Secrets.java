/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.web2;

import com.iexec.sms.secret.Secret;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Web2Secrets {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    private String ownerAddress;
    @OneToMany(cascade = {CascadeType.ALL})
    private List<Secret> secrets;

    Web2Secrets(String ownerAddress) {
        this(ownerAddress, List.of());
    }

    public Web2Secrets(String ownerAddress, List<Secret> secrets) {
        this.ownerAddress = ownerAddress.toLowerCase();
        this.secrets = Collections.unmodifiableList(secrets);
    }

    public Optional<Secret> getSecret(String secretAddress) {
        return secrets.stream()
                .filter(secret -> secret.getAddress().equals(secretAddress))
                .findFirst();
    }

    /**
     * Copies the current {@link Web2Secrets} object, while adding given {@link Secret} to the secrets list.
     * In case of an update,
     * given secret is removed from the secrets list before the new object is created.
     *
     * @param newSecret {@link Secret} to add to the list
     * @return A new {@link Web2Secrets} object with given new {@link Secret}.
     */
    public Web2Secrets withNewSecret(Secret newSecret) {
        List<Secret> newSecrets;
        if (newSecret.getId() == null) {
            // New secret, no need to remove any old corresponding secret.
            newSecrets = new ArrayList<>(secrets);
        } else {
            // Update of existing secret, remove the old secret.
            newSecrets = secrets.stream()
                    .filter(secret -> !Objects.equals(secret.getId(), newSecret.getId()))
                    .collect(Collectors.toList());
        }
        newSecrets.add(newSecret);

        return new Web2Secrets(id, ownerAddress, newSecrets);
    }
}
