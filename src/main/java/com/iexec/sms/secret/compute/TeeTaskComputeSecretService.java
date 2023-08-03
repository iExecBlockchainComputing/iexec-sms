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

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.MeasuredSecretService;
import com.iexec.sms.secret.base.AbstractSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TeeTaskComputeSecretService extends AbstractSecretService<TeeTaskComputeSecret, TeeTaskComputeSecretHeader> {
    protected TeeTaskComputeSecretService(TeeTaskComputeSecretRepository teeTaskComputeSecretRepository,
                                          EncryptionService encryptionService,
                                          MeasuredSecretService computeMeasuredSecretService) {
        super(teeTaskComputeSecretRepository, encryptionService, computeMeasuredSecretService);
    }

    @Override
    protected TeeTaskComputeSecret createSecret(TeeTaskComputeSecretHeader header, String value) {
        return new TeeTaskComputeSecret(header, value);
    }
}
