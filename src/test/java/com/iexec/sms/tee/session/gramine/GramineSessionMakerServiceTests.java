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

package com.iexec.sms.tee.session.gramine;

import com.iexec.common.utils.FileHelper;
import com.iexec.commons.poco.tee.TeeEnclaveConfiguration;
import com.iexec.sms.tee.session.base.SecretEnclaveBase;
import com.iexec.sms.tee.session.base.SecretSessionBase;
import com.iexec.sms.tee.session.base.SecretSessionBaseService;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.gramine.sps.GramineSession;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

import static com.iexec.sms.tee.session.TeeSessionTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class GramineSessionMakerServiceTests {

    @Mock
    private SecretSessionBaseService teeSecretsService;
    @InjectMocks
    private GramineSessionMakerService gramineSessionService;

    // region getSessionYml
    @Test
    void shouldGetSessionJson() throws Exception {
        TeeEnclaveConfiguration enclaveConfig = TeeEnclaveConfiguration.builder()
                .fingerprint(APP_FINGERPRINT)
                .build();
        TeeSessionRequest request = createSessionRequest(createTaskDescription(createDealParams().build(), enclaveConfig).build());

        SecretEnclaveBase appCompute = SecretEnclaveBase.builder()
                .name("app")
                .mrenclave(APP_FINGERPRINT)
                .environment(Map.ofEntries(
                        Map.entry("IEXEC_TASK_ID", "taskId"),
                        Map.entry("IEXEC_IN", "/iexec_in"),
                        Map.entry("IEXEC_OUT", "/iexec_out"),
                        Map.entry("IEXEC_DATASET_ADDRESS", "0xDatasetAddress"),
                        Map.entry("IEXEC_DATASET_FILENAME", "datasetName"),
                        Map.entry("IEXEC_BOT_SIZE", "1"),
                        Map.entry("IEXEC_BOT_FIRST_INDEX", "0"),
                        Map.entry("IEXEC_BOT_TASK_INDEX", "0"),
                        Map.entry("IEXEC_INPUT_FILES_FOLDER", "/iexec_in"),
                        Map.entry("IEXEC_INPUT_FILES_NUMBER", "2"),
                        Map.entry("IEXEC_INPUT_FILE_NAME_1", "file1"),
                        Map.entry("IEXEC_INPUT_FILE_NAME_2", "file2")))
                .build();
        SecretEnclaveBase postCompute = SecretEnclaveBase.builder()
                .name("post-compute")
                .mrenclave(POST_COMPUTE_FINGERPRINT)
                .environment(Map.ofEntries(
                        Map.entry("RESULT_TASK_ID", "taskId"),
                        Map.entry("RESULT_ENCRYPTION", "yes"),
                        Map.entry("RESULT_ENCRYPTION_PUBLIC_KEY", "encryptionPublicKey"),
                        Map.entry("RESULT_STORAGE_PROVIDER", "ipfs"),
                        Map.entry("RESULT_STORAGE_PROXY", "storageProxy"),
                        Map.entry("RESULT_STORAGE_TOKEN", "storageToken"),
                        Map.entry("RESULT_STORAGE_CALLBACK", "no"),
                        Map.entry("RESULT_SIGN_WORKER_ADDRESS", "workerAddress"),
                        Map.entry("RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY", "teeChallengePrivateKey")))
                .build();

        when(teeSecretsService.getSecretsTokens(request))
                .thenReturn(SecretSessionBase.builder()
                        .appCompute(appCompute)
                        .postCompute(postCompute)
                        .build());

        GramineSession actualSpsSession = gramineSessionService.generateSession(request);
        log.info(actualSpsSession.toString());
        Map<String, Object> actualJsonMap = new Yaml().load(actualSpsSession.toString());
        String expectedJsonString = FileHelper.readFile("src/test/resources/gramine-tee-session.json");
        Map<String, Object> expectedYmlMap = new Yaml().load(expectedJsonString);
        assertThat(actualJsonMap)
                .usingRecursiveComparison()
                .isEqualTo(expectedYmlMap);
    }
    // endregion
}
