/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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

import com.iexec.sms.chain.IexecHubService;
import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.MeasuredSecretService;
import com.iexec.sms.tee.config.TeeChallengeCleanupConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class TeeChallengeService {

    private final JdbcTemplate jdbcTemplate;
    private final TeeChallengeRepository teeChallengeRepository;
    private final EncryptionService encryptionService;
    private final IexecHubService iexecHubService;
    private final MeasuredSecretService teeChallengesMeasuredSecretService;
    private final MeasuredSecretService ethereumCredentialsMeasuredSecretService;
    private final TeeChallengeCleanupConfiguration teeChallengeCleanupConfiguration;

    public TeeChallengeService(final JdbcTemplate jdbcTemplate,
                               final TeeChallengeRepository teeChallengeRepository,
                               final EncryptionService encryptionService,
                               final IexecHubService iexecHubService,
                               final MeasuredSecretService teeChallengeMeasuredSecretService,
                               final MeasuredSecretService ethereumCredentialsMeasuredSecretService,
                               final TeeChallengeCleanupConfiguration teeChallengeCleanupConfiguration) {
        this.jdbcTemplate = jdbcTemplate;
        this.teeChallengeRepository = teeChallengeRepository;
        this.encryptionService = encryptionService;
        this.iexecHubService = iexecHubService;
        this.teeChallengesMeasuredSecretService = teeChallengeMeasuredSecretService;
        this.ethereumCredentialsMeasuredSecretService = ethereumCredentialsMeasuredSecretService;
        this.teeChallengeCleanupConfiguration = teeChallengeCleanupConfiguration;
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
            final long finalDeadline = iexecHubService.getTaskDescription(taskId).getFinalDeadline();
            TeeChallenge teeChallenge = new TeeChallenge(taskId, Instant.ofEpochMilli(finalDeadline));
            encryptChallengeKeys(teeChallenge);
            teeChallenge = teeChallengeRepository.save(teeChallenge);
            teeChallengesMeasuredSecretService.newlyAddedSecret();
            ethereumCredentialsMeasuredSecretService.newlyAddedSecret();
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
        final EthereumCredentials credentials = teeChallenge.getCredentials();
        if (!credentials.isEncrypted()) {
            final String encPrivateKey = encryptionService.encrypt(credentials.getPrivateKey());
            credentials.setEncryptedPrivateKey(encPrivateKey);
        }
    }

    public void decryptChallengeKeys(TeeChallenge teeChallenge) {
        final EthereumCredentials credentials = teeChallenge.getCredentials();
        if (credentials.isEncrypted()) {
            final String privateKey = encryptionService.decrypt(credentials.getPrivateKey());
            credentials.setPlainTextPrivateKey(privateKey);
        }
    }

    /**
     * Clean expired tasks challenges at regular intervals.
     * <p>
     * The interval between two consecutive executions is based on the {@code @Scheduled} annotation
     * and its {@code cron} attribute.
     * <p>
     * Look up for entries without deadlines in the database.
     * If entries are found, set deadline to {@code retentionDuration} in the future
     * for up to {@code batchSize} entries.
     *
     * @see <a href="https://protocol.docs.iex.ec/key-concepts/pay-per-task-model>categories in protocol documentation</a>
     */
    @Scheduled(cron = "${tee.challenge.cleanup.cron}")
    void cleanExpiredTasksTeeChallenges() {
        final long start = System.currentTimeMillis();
        teeChallengeRepository.deleteByFinalDeadlineBefore(Instant.now());
        final int remaining = teeChallengeRepository.countByFinalDeadlineIsNull();
        log.info("cleanExpiredTasksTeeChallenges [duration:{}ms, remaining:{}]",
                System.currentTimeMillis() - start, remaining);
        if (remaining == 0) {
            return;
        }
        final int updated = jdbcTemplate.update(
                "UPDATE \"tee_challenge\" SET \"final_deadline\" = ? WHERE \"final_deadline\" IS NULL FETCH FIRST ? ROWS ONLY",
                Instant.now().plus(teeChallengeCleanupConfiguration.getMissingDeadlineRetentionDuration()),
                teeChallengeCleanupConfiguration.getMissingDeadlineMaxBatchSize());
        log.info("cleanExpiredTasksTeeChallenges [duration:{}ms, updated:{}]",
                System.currentTimeMillis() - start, updated);
    }
}
