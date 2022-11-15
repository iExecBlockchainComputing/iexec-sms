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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;

/**
 * @deprecated Use {@link Web2Secret} instead.
 */
@Deprecated(forRemoval = true)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
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
     *
     * @param secretAddress    Address of the new secret.
     * @param secretValue      Value of the new secret.
     * @param isEncryptedValue Whether this value is encrypted.
     * @return A new {@link Web2Secrets} instance, with a new {@link Secret} element.
     * @throws SecretAlreadyExistsException thrown when a secret with same address already exists.
     */
    // TODO: remove `isEncryptedValue` as we only have encrypted values there.
    public Web2Secrets addNewSecret(String secretAddress, String secretValue, boolean isEncryptedValue)
            throws SecretAlreadyExistsException {
        // A new secret can't already exist
        if (getSecret(secretAddress).isPresent()) {
            throw new SecretAlreadyExistsException(ownerAddress, secretAddress);
        }

        final List<Secret> newSecrets = new ArrayList<>(this.secrets);
        newSecrets.add(new Secret(secretAddress, secretValue, isEncryptedValue));
        return new Web2Secrets(id, ownerAddress, newSecrets);
    }

    /**
     * Copies the current {@link Web2Secrets} object, while updating {@link Secret} at given {@code secretAddress}.
     *
     * @param secretAddress    Address of the secret to update.
     * @param newSecretValue   New value for the secret.
     * @param isEncryptedValue Whether this value is encrypted.
     * @return A new {@link Web2Secrets} instance, with a {@link Secret} whose value has been updated.
     * @throws NotAnExistingSecretException thrown when requested address is not known.
     * @throws SameSecretException          thrown when secret has already given value.
     */
    public Web2Secrets updateSecret(String secretAddress, String newSecretValue, boolean isEncryptedValue)
            throws NotAnExistingSecretException, SameSecretException {
        final Optional<Secret> oSecretToUpdate = getSecret(secretAddress);

        // Can't update a secret that doesn't exist
        if (oSecretToUpdate.isEmpty()) {
            throw new NotAnExistingSecretException(ownerAddress, secretAddress);
        }

        // No need to update if same value
        final Secret secretToUpdate = oSecretToUpdate.get();
        if (secretToUpdate.isEncryptedValue() == isEncryptedValue && Objects.equals(secretToUpdate.getValue(), newSecretValue)) {
            throw new SameSecretException(ownerAddress, secretAddress);
        }

        final List<Secret> updatedSecretsList = new ArrayList<>(secrets);
        // Filtering out old secret
        updatedSecretsList.remove(secretToUpdate);
        // Adding updated secret
        updatedSecretsList.add(secretToUpdate.withEncryptedValue(newSecretValue));

        return new Web2Secrets(id, ownerAddress, updatedSecretsList);
    }
}
