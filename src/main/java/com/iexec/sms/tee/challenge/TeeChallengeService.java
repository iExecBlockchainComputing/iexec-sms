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

package com.iexec.sms.tee.challenge;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.MeasuredSecretService;
import com.iexec.sms.utils.EthereumCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class TeeChallengeService {

    private final TeeChallengeRepository teeChallengeRepository;
    private final EncryptionService encryptionService;
    private final MeasuredSecretService measuredSecretService;

    public TeeChallengeService(TeeChallengeRepository teeChallengeRepository,
                               EncryptionService encryptionService,
                               MeasuredSecretService teeChallengeMeasuredSecretService) {
        this.teeChallengeRepository = teeChallengeRepository;
        this.encryptionService = encryptionService;
        this.measuredSecretService = teeChallengeMeasuredSecretService;
    }

    public Optional<TeeChallenge> getOrCreate(String taskId, boolean shouldDecryptKeys) {
        // if existing returns from the db
        Optional<TeeChallenge> optionalTeeChallenge = teeChallengeRepository.findByTaskId(taskId);
        if (optionalTeeChallenge.isPresent()) {
            if (shouldDecryptKeys) { //eventually decrypt if wanted
                decryptChallengeKeys(optionalTeeChallenge.get());
            }
            return optionalTeeChallenge;
        }

        // otherwise create it
        try {
            TeeChallenge teeChallenge = new TeeChallenge(taskId);
            encryptChallengeKeys(teeChallenge);
            teeChallenge = teeChallengeRepository.save(teeChallenge);
            measuredSecretService.newlyAddedSecret();
            log.info("Created tee challenge [chainTaskId:{}, teeChallenge:{}]",
                    taskId, teeChallenge.getCredentials().getAddress());

            if (shouldDecryptKeys) { //eventually decrypt if wanted
                decryptChallengeKeys(teeChallenge);
            }

            return Optional.of(teeChallenge);
        } catch (Exception e) {
            log.error("Couldn't create credentials [exception:{}]", e.getMessage());
            return Optional.empty();
        }
    }

    public void encryptChallengeKeys(TeeChallenge teeChallenge) {
        EthereumCredentials credentials = teeChallenge.getCredentials();
        if (!credentials.isEncrypted()) {
            String encPrivateKey = encryptionService.encrypt(credentials.getPrivateKey());
            credentials.setEncryptedPrivateKey(encPrivateKey);
        }
    }

    public void decryptChallengeKeys(TeeChallenge teeChallenge) {
        EthereumCredentials credentials = teeChallenge.getCredentials();
        if (credentials.isEncrypted()) {
            String privateKey = encryptionService.decrypt(credentials.getPrivateKey());
            credentials.setPlainTextPrivateKey(privateKey);
        }
    }
}
