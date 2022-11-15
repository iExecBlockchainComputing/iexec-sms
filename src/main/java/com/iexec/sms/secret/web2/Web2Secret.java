package com.iexec.sms.secret.web2;

import com.iexec.sms.secret.Secret;
import lombok.*;

import javax.persistence.Entity;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Web2Secret extends Secret {
    private String ownerAddress;

    Web2Secret(String id, String ownerAddress, String address, String value, boolean isEncryptedValue) {
        super(id, address, value, isEncryptedValue);
        this.ownerAddress = ownerAddress;
    }

    public Web2Secret(String ownerAddress, String address, String value, boolean isEncryptedValue) {
        super(address, value, isEncryptedValue);
        this.ownerAddress = ownerAddress;
    }

    /**
     * Copies the current {@link Web2Secret} object,
     * while replacing the old value with a new encrypted value.
     *
     * @param newValue Value to use for new object.
     * @return A new {@link Web2Secret} object with new value.
     */
    public Web2Secret withEncryptedValue(String newValue) {
        return new Web2Secret(this.getId(), this.getOwnerAddress(), this.getAddress(), newValue, true);
    }

    /**
     * Copies the current {@link Web2Secret} object,
     * while replacing the old value with a new decrypted value.
     *
     * @param newValue Value to use for new object.
     * @return A new {@link Web2Secret} object with new value.
     */
    public Web2Secret withDecryptedValue(String newValue) {
        return new Web2Secret(this.getId(), this.getOwnerAddress(), this.getAddress(), newValue, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Web2Secret that = (Web2Secret) o;
        return Objects.equals(ownerAddress, that.ownerAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ownerAddress);
    }
}
