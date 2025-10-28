/*
 * Copyright 2022-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.base;

import com.iexec.common.utils.IexecEnvUtils;
import com.iexec.common.utils.IexecFileHelper;
import com.iexec.commons.poco.chain.ChainDataset;
import com.iexec.commons.poco.chain.DealParams;
import com.iexec.commons.poco.order.DatasetOrder;
import com.iexec.commons.poco.task.TaskDescription;
import com.iexec.commons.poco.tee.TeeEnclaveConfiguration;
import com.iexec.sms.chain.IexecHubService;
import com.iexec.sms.secret.compute.*;
import com.iexec.sms.secret.web2.Web2Secret;
import com.iexec.sms.secret.web2.Web2SecretHeader;
import com.iexec.sms.secret.web2.Web2SecretService;
import com.iexec.sms.secret.web3.Web3SecretService;
import com.iexec.sms.tee.bulk.IpfsClient;
import com.iexec.sms.tee.challenge.EthereumCredentials;
import com.iexec.sms.tee.challenge.TeeChallenge;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.base.SecretEnclaveBase.SecretEnclaveBaseBuilder;
import com.iexec.sms.tee.session.base.SecretSessionBase.SecretSessionBaseBuilder;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.gramine.GramineSessionMakerService;
import com.iexec.sms.tee.session.scone.SconeSessionMakerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.*;

import static com.iexec.common.worker.tee.TeeSessionEnvironmentVariable.*;
import static com.iexec.commons.poco.chain.DealParams.DROPBOX_RESULT_STORAGE_PROVIDER;
import static com.iexec.sms.api.TeeSessionGenerationError.*;
import static com.iexec.sms.secret.ReservedSecretKeyName.*;

/**
 * Service to fetch secrets from SMS database in order to prepare TEE tasks sessions for CAS or SPS.
 *
 * @see GramineSessionMakerService
 * @see SconeSessionMakerService
 */
@Slf4j
@Service
public class SecretSessionBaseService {

    static final String EMPTY_STRING_VALUE = "";
    static final BigInteger BULK_DATASET_VOLUME = BigInteger.TWO.pow(53).subtract(BigInteger.ONE);
    static final String IEXEC_DATASET_PREFIX = "IEXEC_DATASET_";
    static final String IEXEC_DATASET_URL_SUFFIX = "_URL";
    static final String IEXEC_DATASET_CHECKSUM_SUFFIX = "_CHECKSUM";
    static final String IEXEC_DATASET_KEY_SUFFIX = "_KEY";
    static final String IEXEC_DATASET_FILENAME_SUFFIX = "_FILENAME";
    static final String IEXEC_APP_DEVELOPER_SECRET_PREFIX = "IEXEC_APP_DEVELOPER_SECRET_";
    static final String IEXEC_REQUESTER_SECRET_PREFIX = "IEXEC_REQUESTER_SECRET_";

    private final IpfsClient ipfsClient;
    private final IexecHubService iexecHubService;
    private final Web3SecretService web3SecretService;
    private final Web2SecretService web2SecretService;
    private final TeeChallengeService teeChallengeService;
    private final TeeTaskComputeSecretService teeTaskComputeSecretService;

    public SecretSessionBaseService(
            final IpfsClient ipfsClient,
            final IexecHubService iexecHubService,
            final Web3SecretService web3SecretService,
            final Web2SecretService web2SecretService,
            final TeeChallengeService teeChallengeService,
            final TeeTaskComputeSecretService teeTaskComputeSecretService) {
        this.iexecHubService = iexecHubService;
        this.web3SecretService = web3SecretService;
        this.web2SecretService = web2SecretService;
        this.teeChallengeService = teeChallengeService;
        this.teeTaskComputeSecretService = teeTaskComputeSecretService;
        this.ipfsClient = ipfsClient;
    }

    /**
     * Collect tokens required for different compute stages (pre, app, post).
     *
     * @param request Session request details
     * @return All common tokens for a session, whatever TEE technology is used
     */
    public SecretSessionBase getSecretsTokens(final TeeSessionRequest request) throws TeeSessionGenerationException {
        if (request == null) {
            throw new TeeSessionGenerationException(
                    NO_SESSION_REQUEST,
                    "Session request must not be null");
        }
        // Task description or deal params should never be null
        // We nevertheless add both checks to cover NullPointerException
        if (request.getTaskDescription() == null || request.getTaskDescription().getDealParams() == null) {
            throw new TeeSessionGenerationException(
                    NO_TASK_DESCRIPTION,
                    "Task description and deal parameters must both not be null");
        }
        final SecretSessionBaseBuilder sessionBase = SecretSessionBase.builder();
        final TaskDescription taskDescription = request.getTaskDescription();
        final Map<String, String> signTokens = getSignTokens(request);
        // pre-compute
        if (taskDescription.requiresPreCompute()) {
            sessionBase.preCompute(getPreComputeTokens(request, signTokens));
        }
        // app
        sessionBase.appCompute(getAppTokens(request));
        // post compute
        sessionBase.postCompute(getPostComputeTokens(request, signTokens));
        return sessionBase.build();
    }

    /**
     * Get tokens required for different signature-related features.
     *
     * @param request Session request details
     * @return A {@code Map} containing tokens required for the signature
     * @throws TeeSessionGenerationException if any of the required tokens is missing
     */
    Map<String, String> getSignTokens(final TeeSessionRequest request) throws TeeSessionGenerationException {
        final String taskId = request.getTaskDescription().getChainTaskId();
        final String workerAddress = request.getWorkerAddress();
        if (StringUtils.isEmpty(workerAddress)) {
            throw new TeeSessionGenerationException(
                    GET_SIGNATURE_TOKENS_FAILED_EMPTY_WORKER_ADDRESS,
                    "Empty worker address - taskId: " + taskId);
        }
        if (StringUtils.isEmpty(request.getEnclaveChallenge())) {
            throw new TeeSessionGenerationException(
                    GET_SIGNATURE_TOKENS_FAILED_EMPTY_PUBLIC_ENCLAVE_CHALLENGE,
                    "Empty public enclave challenge - taskId: " + taskId);
        }
        final Optional<TeeChallenge> teeChallenge = teeChallengeService.getOrCreate(taskId, true);
        if (teeChallenge.isEmpty()) {
            throw new TeeSessionGenerationException(
                    GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CHALLENGE,
                    "Empty TEE challenge  - taskId: " + taskId);
        }
        final EthereumCredentials enclaveCredentials = teeChallenge.get().getCredentials();
        if (enclaveCredentials == null || enclaveCredentials.getPrivateKey().isEmpty()) {
            throw new TeeSessionGenerationException(
                    GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CREDENTIALS,
                    "Empty TEE challenge credentials - taskId: " + taskId);
        }
        return Map.of(
                IEXEC_TASK_ID.name(), taskId,
                SIGN_WORKER_ADDRESS.name(), workerAddress,
                SIGN_TEE_CHALLENGE_PRIVATE_KEY.name(), enclaveCredentials.getPrivateKey()
        );
    }

    // region pre-compute

    /**
     * Fetch dataset orders related to a bulk processing slice from IPFS
     *
     * @param taskDescription A task part of a bulk processing deal and corresponding to one of the slices
     * @return The list of {@code DatasetOrder} found in the slice, or an empty list if any issue arises
     */
    private List<DatasetOrder> fetchDatasetOrders(final TaskDescription taskDescription) {
        try {
            final String bulkCid = taskDescription.getDealParams().getBulkCid();
            final int bulkSliceIndex = taskDescription.getBotIndex();
            log.info("Fetching dataset orders for a bulk slice [chainTaskId:{}, bulkCid:{}, bulkSliceIndex:{}]",
                    taskDescription.getChainTaskId(), bulkCid, bulkSliceIndex);
            final List<String> bulkSlices = ipfsClient.readBulkCid(bulkCid);
            final List<DatasetOrder> tempList = ipfsClient.readOrders(bulkSlices.get(bulkSliceIndex));
            return List.copyOf(tempList);
        } catch (final Exception e) {
            log.error("Error during bulk computation", e);
            return List.of();
        }
    }

    private Map<String, Object> getBulkDatasetTokens(final int index,
                                                     final TaskDescription taskDescription,
                                                     final DatasetOrder datasetOrder) {
        final String prefix = IEXEC_DATASET_PREFIX + (index + 1);
        final ChainDataset dataset = iexecHubService.getChainDataset(datasetOrder.getDataset()).orElse(null);
        if (dataset != null && isBulkDatasetOrderCompatibleWithDeal(datasetOrder, taskDescription)) {
            final String datasetKey = web3SecretService.getDecryptedValue(datasetOrder.getDataset()).orElse("");
            return Map.of(
                    prefix + IEXEC_DATASET_URL_SUFFIX, dataset.getMultiaddr(),
                    prefix + IEXEC_DATASET_CHECKSUM_SUFFIX, dataset.getChecksum(),
                    prefix + IEXEC_DATASET_KEY_SUFFIX, datasetKey,
                    prefix + IEXEC_DATASET_FILENAME_SUFFIX, datasetOrder.getDataset()
            );
        } else {
            return Map.of(
                    prefix + IEXEC_DATASET_URL_SUFFIX, EMPTY_STRING_VALUE,
                    prefix + IEXEC_DATASET_CHECKSUM_SUFFIX, EMPTY_STRING_VALUE,
                    prefix + IEXEC_DATASET_KEY_SUFFIX, EMPTY_STRING_VALUE,
                    prefix + IEXEC_DATASET_FILENAME_SUFFIX, datasetOrder.getDataset()
            );
        }
    }

    boolean isBulkDatasetOrderCompatibleWithDeal(final DatasetOrder datasetOrder, final TaskDescription taskDescription) {
        try {
            log.debug("Check dataset order against deal [chainTaskId:{}, deal:{}, dataset:{}",
                    taskDescription.getChainTaskId(), taskDescription.getChainDealId(), datasetOrder.getDataset());
            iexecHubService.assertDatasetDealCompatibility(datasetOrder, taskDescription.getChainDealId());
            return true;
        } catch (Exception e) {
            log.error("Failed to perform all checks on dataset [chainTaskId:{}, dataset:{}, error:{}]",
                    taskDescription.getChainTaskId(), datasetOrder.getDataset(), e.getMessage());
            return false;
        }
    }

    /**
     * Get tokens to be injected in the pre-compute enclave.
     *
     * @param request    Session request details
     * @param signTokens Tokens required for the signature of the communications with the worker
     * @return A {@link SecretEnclaveBase} instance
     * @throws TeeSessionGenerationException if dataset secret is not found
     */
    SecretEnclaveBase getPreComputeTokens(final TeeSessionRequest request, final Map<String, String> signTokens) throws TeeSessionGenerationException {
        final SecretEnclaveBaseBuilder enclaveBase = SecretEnclaveBase.builder();
        enclaveBase.name("pre-compute");
        final Map<String, Object> tokens = new HashMap<>();
        final TaskDescription taskDescription = request.getTaskDescription();
        final String taskId = taskDescription.getChainTaskId();
        enclaveBase.mrenclave(request.getTeeServicesProperties().getPreComputeProperties().getFingerprint());
        tokens.put(IEXEC_PRE_COMPUTE_OUT.name(), IexecFileHelper.SLASH_IEXEC_IN);
        // `IS_DATASET_REQUIRED` still meaningful?
        tokens.put(IS_DATASET_REQUIRED.name(), taskDescription.containsDataset());

        if (taskDescription.isBulkRequest()) {
            final List<DatasetOrder> orders = fetchDatasetOrders(taskDescription);
            tokens.put(IEXEC_BULK_SLICE_SIZE.name(), orders.size());
            for (int i = 0; i < orders.size(); i++) {
                final DatasetOrder order = orders.get(i);
                tokens.putAll(getBulkDatasetTokens(i, taskDescription, order));
            }
        }

        final List<String> trustedEnv = new ArrayList<>();
        if (taskDescription.containsDataset()) {
            final String datasetKey = web3SecretService
                    .getDecryptedValue(taskDescription.getDatasetAddress())
                    .orElseThrow(() -> new TeeSessionGenerationException(
                            PRE_COMPUTE_GET_DATASET_SECRET_FAILED,
                            "Empty dataset secret - taskId: " + taskId));
            tokens.put(IEXEC_DATASET_KEY.name(), datasetKey);
            trustedEnv.addAll(List.of(
                    IEXEC_DATASET_URL.name(),
                    IEXEC_DATASET_FILENAME.name(),
                    IEXEC_DATASET_CHECKSUM.name()));
        } else {
            log.info("No dataset key needed for this task [taskId:{}]", taskId);
        }
        trustedEnv.addAll(List.of(
                IEXEC_DEAL_ID.name(),
                IEXEC_TASK_INDEX.name(),
                IEXEC_TASK_ID.name(),
                IEXEC_INPUT_FILES_FOLDER.name(),
                IEXEC_INPUT_FILES_NUMBER.name()));
        IexecEnvUtils.getAllIexecEnv(taskDescription)
                .entrySet()
                .stream()
                .filter(e ->
                        // extract trusted en vars to include
                        trustedEnv.contains(e.getKey())
                                // extract <IEXEC_INPUT_FILE_URL_N, url>
                                || e.getKey().startsWith(IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX))
                .forEach(e -> tokens.put(e.getKey(), e.getValue()));

        // for communication with worker
        tokens.putAll(signTokens);

        return enclaveBase
                .environment(tokens)
                .build();
    }

    // endregion

    // region app-compute

    /**
     * Get tokens to be injected in the application enclave.
     *
     * @param request Session request details
     * @return A {@link SecretEnclaveBase} instance
     * @throws TeeSessionGenerationException if {@code TaskDescription} is {@literal null} or does not contain a {@code TeeEnclaveConfiguration}
     */
    SecretEnclaveBase getAppTokens(final TeeSessionRequest request) throws TeeSessionGenerationException {
        final SecretEnclaveBaseBuilder enclaveBase = SecretEnclaveBase.builder();
        enclaveBase.name("app");
        final TaskDescription taskDescription = request.getTaskDescription();

        final Map<String, Object> tokens = new HashMap<>();
        final TeeEnclaveConfiguration enclaveConfig = taskDescription.getAppEnclaveConfiguration();
        if (enclaveConfig == null) {
            throw new TeeSessionGenerationException(
                    APP_COMPUTE_NO_ENCLAVE_CONFIG,
                    "Enclave configuration must not be null");
        }
        if (!enclaveConfig.getValidator().isValid()) {
            throw new TeeSessionGenerationException(
                    APP_COMPUTE_INVALID_ENCLAVE_CONFIG,
                    "Invalid enclave configuration: " +
                            enclaveConfig.getValidator().validate().toString());
        }

        enclaveBase.mrenclave(enclaveConfig.getFingerprint());

        final Map<String, Object> computeSecrets = getApplicationComputeSecrets(taskDescription);
        tokens.putAll(computeSecrets);
        // trusted env variables (not confidential)
        tokens.putAll(IexecEnvUtils.getComputeStageEnvMap(taskDescription));

        if (taskDescription.isBulkRequest()) {
            final List<String> addresses = fetchDatasetOrders(taskDescription).stream()
                    .map(DatasetOrder::getDataset)
                    .toList();
            tokens.put(IEXEC_BULK_SLICE_SIZE.name(), addresses.size());
            for (int i = 0; i < addresses.size(); i++) {
                tokens.put(IEXEC_DATASET_PREFIX + (i + 1) + IEXEC_DATASET_FILENAME_SUFFIX, addresses.get(i));
            }
        }

        return enclaveBase
                .environment(tokens)
                .build();
    }

    /**
     * Get secrets defined for the application execution.
     * <p>
     * Application secrets can be of two kinds:
     * <ul>
     * <li>A single application secret defined by the application developer for its application
     * <li>Up to several requester secrets pushed by the requester in the database and mapped to the application in
     * deal parameters
     * </ul>
     *
     * @param taskDescription A task description
     * @return A {@code Map} containing secrets retrieved from the database.
     */
    private Map<String, Object> getApplicationComputeSecrets(final TaskDescription taskDescription) {
        final Map<String, Object> tokens = new HashMap<>();
        final List<TeeTaskComputeSecretHeader> ids = getAppComputeSecretsHeaders(taskDescription);
        log.debug("TeeTaskComputeSecret looking for secrets [chainTaskId:{}, count:{}]",
                taskDescription.getChainTaskId(), ids.size());
        final List<TeeTaskComputeSecret> secrets = teeTaskComputeSecretService.getSecretsForTeeSession(ids);
        log.debug("TeeTaskComputeSecret objects fetched from database [chainTaskId:{}, count:{}]",
                taskDescription.getChainTaskId(), secrets.size());
        for (final TeeTaskComputeSecret secret : secrets) {
            if (!StringUtils.isEmpty(secret.getHeader().getOnChainObjectAddress())) {
                tokens.put("IEXEC_APP_DEVELOPER_SECRET", secret.getValue());
                tokens.put(IEXEC_APP_DEVELOPER_SECRET_PREFIX + "1", secret.getValue());
            } else {
                final String secretKey = secret.getHeader().getKey();
                taskDescription.getDealParams().getIexecSecrets().forEach((key, value) -> {
                    if (value.equals(secretKey)) {
                        tokens.put(IEXEC_REQUESTER_SECRET_PREFIX + key, secret.getValue());
                    }
                });
            }
        }
        return tokens;
    }

    private List<TeeTaskComputeSecretHeader> getAppComputeSecretsHeaders(final TaskDescription taskDescription) {
        final List<TeeTaskComputeSecretHeader> ids = new ArrayList<>();
        final String applicationAddress = taskDescription.getAppAddress();
        if (applicationAddress != null) {
            final String secretIndex = "1";
            ids.add(new TeeTaskComputeSecretHeader(
                    OnChainObjectType.APPLICATION,
                    applicationAddress.toLowerCase(),
                    SecretOwnerRole.APPLICATION_DEVELOPER,
                    EMPTY_STRING_VALUE,
                    secretIndex));
        }

        if (taskDescription.getDealParams().getIexecSecrets() != null && taskDescription.getRequester() != null) {
            for (Map.Entry<String, String> secretEntry : taskDescription.getDealParams().getIexecSecrets().entrySet()) {
                try {
                    final int requesterSecretIndex = Integer.parseInt(secretEntry.getKey());
                    if (requesterSecretIndex <= 0) {
                        final String message = "Application secret indices provided in the deal parameters must be positive numbers"
                                + " [providedApplicationSecretIndex:" + requesterSecretIndex + "]";
                        log.warn(message);
                        throw new NumberFormatException(message);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid entry found in deal parameters secrets map", e);
                    continue;
                }
                ids.add(new TeeTaskComputeSecretHeader(
                        OnChainObjectType.APPLICATION,
                        EMPTY_STRING_VALUE,
                        SecretOwnerRole.REQUESTER,
                        taskDescription.getRequester().toLowerCase(),
                        secretEntry.getValue()));
            }
        }
        return ids;
    }

    // endregion

    // region post-compute

    /**
     * Get tokens to be injected in the post-compute enclave.
     *
     * @param request    Session request details
     * @param signTokens Tokens required for the signature of the communications with the worker
     * @return A {@link SecretEnclaveBase} instance
     * @throws TeeSessionGenerationException if {@code TaskDescription} is {@literal null}
     */
    SecretEnclaveBase getPostComputeTokens(final TeeSessionRequest request, final Map<String, String> signTokens) throws TeeSessionGenerationException {
        final SecretEnclaveBaseBuilder enclaveBase = SecretEnclaveBase.builder()
                .name("post-compute")
                .mrenclave(request.getTeeServicesProperties().getPostComputeProperties().getFingerprint());
        final Map<String, Object> tokens = new HashMap<>();
        final TaskDescription taskDescription = request.getTaskDescription();
        final List<Web2SecretHeader> ids = getPostComputeSecretHeaders(taskDescription, request.getWorkerAddress());
        log.debug("Web2Secret looking for secrets [chainTaskId:{}, count:{}]",
                taskDescription.getChainTaskId(), ids.size());
        final List<Web2Secret> secrets = web2SecretService.getSecretsForTeeSession(ids);
        log.debug("Web2Secret objects fetched from database [chainTaskId:{}, count:{}]",
                taskDescription.getChainTaskId(), secrets.size());
        // encryption
        final String resultEncryptionSecret = secrets.stream()
                .filter(secret -> IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY.equals(secret.getHeader().getAddress()))
                .findFirst()
                .map(Web2Secret::getValue)
                .orElse(EMPTY_STRING_VALUE);
        tokens.putAll(getPostComputeEncryptionTokens(request, resultEncryptionSecret));
        // storage
        if (taskDescription.containsCallback()) {
            tokens.putAll(getPostComputeStorageTokens(request, EMPTY_STRING_VALUE, EMPTY_STRING_VALUE));
        } else if (DROPBOX_RESULT_STORAGE_PROVIDER.equals(taskDescription.getDealParams().getIexecResultStorageProvider())) {
            final String storageToken = secrets.stream()
                    .filter(secret -> IEXEC_RESULT_DROPBOX_TOKEN.equals(secret.getHeader().getAddress()))
                    .findFirst()
                    .map(Web2Secret::getValue)
                    .orElse(EMPTY_STRING_VALUE);
            tokens.putAll(getPostComputeStorageTokens(request, storageToken, EMPTY_STRING_VALUE));
        } else {
            // TODO remove fallback on requester token when legacy Result Proxy endpoints have been removed
            final boolean isWorkerTokenPresent = secrets.stream()
                    .anyMatch(secret -> IEXEC_RESULT_IEXEC_IPFS_TOKEN.equals(secret.getHeader().getAddress())
                            && request.getWorkerAddress().equalsIgnoreCase(secret.getHeader().getOwnerAddress()));
            final String tokenOwner = isWorkerTokenPresent ? request.getWorkerAddress() : taskDescription.getRequester();
            final String storageProxy = secrets.stream()
                    .filter(secret -> IEXEC_RESULT_IEXEC_RESULT_PROXY_URL.equals(secret.getHeader().getAddress()))
                    .findFirst()
                    .map(Web2Secret::getValue)
                    .orElse(EMPTY_STRING_VALUE);
            final String storageToken = secrets.stream()
                    .filter(secret -> IEXEC_RESULT_IEXEC_IPFS_TOKEN.equals(secret.getHeader().getAddress()) &&
                            tokenOwner.equalsIgnoreCase(secret.getHeader().getOwnerAddress()))
                    .findFirst()
                    .map(Web2Secret::getValue)
                    .orElse(EMPTY_STRING_VALUE);
            log.debug("storage token [isWorkerTokenPresent:{}, tokenOwner:{}]",
                    isWorkerTokenPresent, tokenOwner);
            tokens.putAll(getPostComputeStorageTokens(request, storageToken, storageProxy));
        }
        // for communication with worker
        tokens.putAll(signTokens);

        return enclaveBase
                .environment(tokens)
                .build();
    }

    List<Web2SecretHeader> getPostComputeSecretHeaders(final TaskDescription taskDescription, final String workerAddress) {
        final List<Web2SecretHeader> ids = new ArrayList<>();
        if (taskDescription.getDealParams().isIexecResultEncryption()) {
            ids.add(new Web2SecretHeader(taskDescription.getBeneficiary(), IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY));
        }
        if (DROPBOX_RESULT_STORAGE_PROVIDER.equals(taskDescription.getDealParams().getIexecResultStorageProvider())) {
            ids.add(new Web2SecretHeader(taskDescription.getRequester(), IEXEC_RESULT_DROPBOX_TOKEN));
        } else {
            ids.add(new Web2SecretHeader(taskDescription.getRequester(), IEXEC_RESULT_IEXEC_IPFS_TOKEN));
            ids.add(new Web2SecretHeader(workerAddress, IEXEC_RESULT_IEXEC_IPFS_TOKEN));
            ids.add(new Web2SecretHeader(taskDescription.getWorkerpoolOwner(), IEXEC_RESULT_IEXEC_RESULT_PROXY_URL));
        }
        return ids;
    }

    Map<String, String> getPostComputeEncryptionTokens(final TeeSessionRequest request, final String resultEncryptionKey)
            throws TeeSessionGenerationException {
        final TaskDescription taskDescription = request.getTaskDescription();
        final String taskId = taskDescription.getChainTaskId();
        final Map<String, String> tokens = new HashMap<>();
        final boolean shouldEncrypt = taskDescription.getDealParams().isIexecResultEncryption();
        tokens.put(RESULT_ENCRYPTION.name(), Boolean.toString(shouldEncrypt));
        tokens.put(RESULT_ENCRYPTION_PUBLIC_KEY.name(), EMPTY_STRING_VALUE);
        if (!shouldEncrypt) {
            return tokens;
        }
        if (StringUtils.isEmpty(resultEncryptionKey)) {
            throw new TeeSessionGenerationException(
                    POST_COMPUTE_GET_ENCRYPTION_TOKENS_FAILED_EMPTY_BENEFICIARY_KEY,
                    "Empty beneficiary encryption key - taskId: " + taskId);
        }
        tokens.put(RESULT_ENCRYPTION_PUBLIC_KEY.name(), resultEncryptionKey); // base64 encoded by client
        return tokens;
    }

    // TODO: We need a signature of the beneficiary to push
    // to the beneficiary private storage space waiting for
    // that feature we only allow to push to the requester
    // private storage space
    Map<String, String> getPostComputeStorageTokens(final TeeSessionRequest request,
                                                    final String storageToken,
                                                    final String resultProxyUrl) throws TeeSessionGenerationException {
        final TaskDescription taskDescription = request.getTaskDescription();
        final String taskId = taskDescription.getChainTaskId();
        final Map<String, String> tokens = new HashMap<>();
        final boolean isCallbackRequested = taskDescription.containsCallback();
        tokens.put(RESULT_STORAGE_CALLBACK.name(), Boolean.toString(isCallbackRequested));
        tokens.put(RESULT_STORAGE_PROVIDER.name(), EMPTY_STRING_VALUE);
        tokens.put(RESULT_STORAGE_PROXY.name(), EMPTY_STRING_VALUE);
        tokens.put(RESULT_STORAGE_TOKEN.name(), EMPTY_STRING_VALUE);
        if (isCallbackRequested) {
            return tokens;
        }
        final DealParams dealParams = taskDescription.getDealParams();
        final String storageProvider = dealParams.getIexecResultStorageProvider();
        final String storageProxy = dealParams.getIexecResultStorageProxy() != null ?
                dealParams.getIexecResultStorageProxy() : resultProxyUrl;
        if (StringUtils.isEmpty(storageToken)) {
            log.error("Failed to get storage token [taskId:{}, storageProvider:{}, requester:{}]",
                    taskId, storageProvider, taskDescription.getRequester());
            throw new TeeSessionGenerationException(
                    POST_COMPUTE_GET_STORAGE_TOKENS_FAILED,
                    "Empty requester storage token - taskId: " + taskId);
        }
        tokens.put(RESULT_STORAGE_PROVIDER.name(), storageProvider);
        tokens.put(RESULT_STORAGE_PROXY.name(), storageProxy);
        tokens.put(RESULT_STORAGE_TOKEN.name(), storageToken);
        return tokens;
    }

    // endregion

}
