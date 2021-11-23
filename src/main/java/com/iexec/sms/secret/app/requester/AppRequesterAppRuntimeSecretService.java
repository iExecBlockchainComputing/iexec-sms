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

package com.iexec.sms.secret.app.requester;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.app.AbstractAppRuntimeSecretService;
import com.iexec.sms.secret.app.AppRuntimeSecretCountRepository;
import com.iexec.sms.secret.app.AppRuntimeSecretOwnerRole;
import com.iexec.sms.secret.app.AppRuntimeSecretRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppRequesterAppRuntimeSecretService extends AbstractAppRuntimeSecretService {

    public AppRequesterAppRuntimeSecretService(AppRuntimeSecretRepository appRuntimeSecretRepository,
                                               AppRuntimeSecretCountRepository appRuntimeSecretCountRepository,
                                               EncryptionService encryptionService) {
        super(
                appRuntimeSecretRepository,
                appRuntimeSecretCountRepository,
                encryptionService,
                AppRuntimeSecretOwnerRole.REQUESTER
        );
    }
}
