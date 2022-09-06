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
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@Getter
@Entity
@NoArgsConstructor
public class Web2Secrets {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    private String ownerAddress;
    @OneToMany(cascade = {CascadeType.ALL})
    private List<Secret> secrets;

    Web2Secrets(String ownerAddress) {
        this.ownerAddress = ownerAddress;
        this.secrets = new ArrayList<>();
    }

    public Optional<Secret> getSecret(String secretAddress) {
        return secrets.stream()
                .filter(secret -> secret.getAddress().equals(secretAddress))
                .findFirst();
    }
}
