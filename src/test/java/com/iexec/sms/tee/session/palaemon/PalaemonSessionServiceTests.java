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

package com.iexec.sms.tee.session.palaemon;

import com.iexec.common.chain.ChainApp;
import com.iexec.common.chain.ChainDataset;
import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.DealParams;
import com.iexec.common.precompute.PreComputeUtils;
import com.iexec.common.sms.secret.ReservedSecretKeyName;
import com.iexec.common.worker.result.ResultUtils;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.secret.Secret;
import com.iexec.sms.secret.web2.Web2SecretsService;
import com.iexec.sms.secret.web3.Web3Secret;
import com.iexec.sms.secret.web3.Web3SecretService;
import com.iexec.sms.tee.challenge.TeeChallenge;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.precompute.PreComputeConfig;
import com.iexec.sms.utils.EthereumCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class PalaemonSessionServiceTests {

    private static final String DEAL_ID = "dealId";
    private static final String TASK_ID = "taskId";
    private static final String SESSION_ID = "sessionId";
    private static final String WORKER_ADDRESS = "workerAddress";
    private static final String ENCLAVE_CHALLENGE = "enclaveChallenge";
    private static final String REQUESTER = "requester";
    // pre-compute
    private static final String PRE_COMPUTE_FINGERPRINT = "fspfKey2|fspfTag2|mrEnclave2";
    private static final String[] PRE_COMPUTE_FINGERPRINT_PARTS =
    PRE_COMPUTE_FINGERPRINT.split("\\|");
    private static final String DATASET_ID = "datasetId";
    private static final String DATASET_CHECKSUM = "datasetChecksum";
    // app
    private static final String APP_ID = "appId";
    private static final String APP_FINGERPRINT = "fspfKey1|fspfTag1|mrEnclave1|entryPoint";
    private static final String[] APP_FINGERPRINT_PARTS = APP_FINGERPRINT.split("\\|");
    private static final String ARGS = "args";
    // post-compute
    private static final String POST_COMPUTE_FINGERPRINT = "fspfKey3|fspfTag3|mrEnclave3";
    private static final String[] POST_COMPUTE_FINGERPRINT_PARTS =
            POST_COMPUTE_FINGERPRINT.split("\\|");
    private static final String POST_COMPUTE_IMAGE = "postComputeImage";
    private static final String STORAGE_PROVIDER = "ipfs";
    private static final String STORAGE_PROXY = "storageProxy";
    private static final String STORAGE_TOKEN = "storageToken";
    private static final String PUBLIC_KEY = "publicKey";

    @Mock
    private IexecHubService iexecHubService;
    @Mock
    private Web3SecretService web3SecretService;
    @Mock
    private Web2SecretsService web2SecretsService;
    @Mock
    private TeeChallengeService teeChallengeService;
    @Mock
    private PreComputeConfig preComputeConfig;

    @Spy
    @InjectMocks
    private PalaemonSessionService palaemonSessionService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    // pre-compute

    @Test
    public void shouldGetPreComputePalaemonTokens() throws Exception {
        PalaemonSessionRequest request = createSessionRequest();
        when(preComputeConfig.getFingerprint()).thenReturn(PRE_COMPUTE_FINGERPRINT);
        Web3Secret secret = new Web3Secret("address", "value");
        when(web3SecretService.getSecret(DATASET_ID, true))
                .thenReturn(Optional.of(secret));

        Map<String, String> tokens =
                palaemonSessionService.getPreComputePalaemonTokens(request);
        assertThat(tokens).isNotEmpty();
        assertThat(tokens.get(PalaemonSessionService.PRE_COMPUTE_FSPF_KEY))
                .isEqualTo(PRE_COMPUTE_FINGERPRINT_PARTS[0]);
        assertThat(tokens.get(PalaemonSessionService.PRE_COMPUTE_FSPF_TAG))
                .isEqualTo(PRE_COMPUTE_FINGERPRINT_PARTS[1]);
        assertThat(tokens.get(PalaemonSessionService.PRE_COMPUTE_MRENCLAVE))
                .isEqualTo(PRE_COMPUTE_FINGERPRINT_PARTS[2]);
        assertThat(tokens.get(PreComputeUtils.IEXEC_DATASET_CHECKSUM_PROPERTY))
                .isEqualTo(DATASET_CHECKSUM);
        assertThat(tokens.get(PreComputeUtils.IEXEC_DATASET_KEY_PROPERTY))
                .isEqualTo(secret.getValue());
    }

    // app

    @Test
    public void shouldGetAppPalaemonTokens() throws Exception {
        PalaemonSessionRequest request = createSessionRequest();
        Map<String, String> tokens =
                palaemonSessionService.getAppPalaemonTokens(request);
        assertThat(tokens).isNotEmpty();
        assertThat(tokens.get(PalaemonSessionService.APP_FSPF_KEY))
                .isEqualTo(APP_FINGERPRINT_PARTS[0]);
        assertThat(tokens.get(PalaemonSessionService.APP_FSPF_TAG))
                .isEqualTo(APP_FINGERPRINT_PARTS[1]);
        assertThat(tokens.get(PalaemonSessionService.APP_MRENCLAVE))
                .isEqualTo(APP_FINGERPRINT_PARTS[2]);
        assertThat(tokens.get(PalaemonSessionService.APP_ARGS))
                .isEqualTo(APP_FINGERPRINT_PARTS[3] + " " + ARGS);
    }

    // post-compute

    @Test
    public void shouldGetPostComputePalaemonTokens() throws Exception {
        PalaemonSessionRequest request = createSessionRequest();
        Secret publicKeySecret = new Secret("address", PUBLIC_KEY);
        when(web2SecretsService.getSecret(
                request.getChainDeal().getBeneficiary(),
                ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY,
                true))
                .thenReturn(Optional.of(publicKeySecret));
        Secret storageSecret = new Secret("address", STORAGE_TOKEN);
        when(web2SecretsService.getSecret(
                REQUESTER,
                ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN,
                true))
                .thenReturn(Optional.of(storageSecret));
        
        TeeChallenge challenge = TeeChallenge.builder()
                .credentials(new EthereumCredentials())
                .build();
        when(teeChallengeService.getOrCreate(TASK_ID, true))
                .thenReturn(Optional.of(challenge));


        Map<String, String> tokens =
                palaemonSessionService.getPostComputePalaemonTokens(request);
        assertThat(tokens).isNotEmpty();
        assertThat(tokens.get(PalaemonSessionService.POST_COMPUTE_FSPF_KEY))
                .isEqualTo(POST_COMPUTE_FINGERPRINT_PARTS[0]);
        assertThat(tokens.get(PalaemonSessionService.POST_COMPUTE_FSPF_TAG))
                .isEqualTo(POST_COMPUTE_FINGERPRINT_PARTS[1]);
        assertThat(tokens.get(PalaemonSessionService.POST_COMPUTE_MRENCLAVE))
                .isEqualTo(POST_COMPUTE_FINGERPRINT_PARTS[2]);
        // encryption tokens
        assertThat(tokens.get(ResultUtils.RESULT_ENCRYPTION)).isEqualTo("yes") ;
        assertThat(tokens.get(ResultUtils.RESULT_ENCRYPTION_PUBLIC_KEY))
                .isEqualTo(PUBLIC_KEY);
        // storage tokens
        assertThat(tokens.get(ResultUtils.RESULT_STORAGE_CALLBACK)).isEqualTo("no");
        assertThat(tokens.get(ResultUtils.RESULT_STORAGE_PROVIDER))
                .isEqualTo(STORAGE_PROVIDER);
        assertThat(tokens.get(ResultUtils.RESULT_STORAGE_PROXY))
                .isEqualTo(STORAGE_PROXY);
        assertThat(tokens.get(ResultUtils.RESULT_STORAGE_TOKEN))
                .isEqualTo(STORAGE_TOKEN);
        // sign tokens
        assertThat(tokens.get(ResultUtils.RESULT_TASK_ID)).isEqualTo(TASK_ID);
        assertThat(tokens.get(ResultUtils.RESULT_SIGN_WORKER_ADDRESS))
                .isEqualTo(WORKER_ADDRESS);
        assertThat(tokens.get(ResultUtils.RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY))
                .isEqualTo(challenge.getCredentials().getPrivateKey());
    }

    private PalaemonSessionRequest createSessionRequest() {
        return PalaemonSessionRequest.builder()
                .chainTaskId(TASK_ID)
                .sessionId(SESSION_ID)
                .workerAddress(WORKER_ADDRESS)
                .chainDeal(createChainDeal())
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();
    }

    private ChainDeal createChainDeal() {
        ChainApp chainApp = ChainApp.builder()
                .chainAppId(APP_ID)
                .fingerprint(APP_FINGERPRINT)
                .build();
        ChainDataset chainDataset = ChainDataset.builder()
                .chainDatasetId(DATASET_ID)
                .checksum(DATASET_CHECKSUM)
                .build();
        return ChainDeal.builder()
                .chainDealId(DEAL_ID)
                .chainApp(chainApp)
                .chainDataset(chainDataset)
                .requester(REQUESTER)
                .params(DealParams.builder()
                        .iexecResultEncryption(true)
                        .iexecArgs(ARGS)
                        .iexecResultStorageProvider(STORAGE_PROVIDER)
                        .iexecResultStorageProxy(STORAGE_PROXY)
                        .iexecTeePostComputeFingerprint(POST_COMPUTE_FINGERPRINT)
                        .iexecTeePostComputeImage(POST_COMPUTE_IMAGE)
                        .build())
                .build();
    }
}
