package com.iexec.sms.tee.session.gramine;

import com.iexec.common.tee.TeeEnclaveConfiguration;
import com.iexec.common.utils.FileHelper;
import com.iexec.sms.tee.session.EnclaveEnvironment;
import com.iexec.sms.tee.session.EnclaveEnvironments;
import com.iexec.sms.tee.session.TeeSecretsService;
import com.iexec.sms.tee.session.generic.TeeSecretsSessionRequest;
import com.iexec.sms.tee.session.gramine.sps.SpsSession;
import com.iexec.sms.tee.workflow.TeeWorkflowConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

import java.util.Map;

import static com.iexec.sms.tee.session.TeeSessionTestUtils.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class GramineSessionMakerServiceTests {
    @Mock
    private TeeWorkflowConfiguration teeWorkflowConfig;
    @Mock
    private TeeSecretsService teeSecretsService;
    @InjectMocks
    private GramineSessionMakerService gramineSessionService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    // region getSessionYml
    @Test
    void shouldGetSessionJson() throws Exception {
        TeeEnclaveConfiguration enclaveConfig = mock(TeeEnclaveConfiguration.class);
        TeeSecretsSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));

        when(teeWorkflowConfig.getPostComputeFingerprint()).thenReturn(POST_COMPUTE_FINGERPRINT);
        when(teeWorkflowConfig.getPostComputeEntrypoint()).thenReturn(POST_COMPUTE_ENTRYPOINT);
        when(enclaveConfig.getFingerprint()).thenReturn(APP_FINGERPRINT);
        when(enclaveConfig.getEntrypoint()).thenReturn("/apploader.sh");

        EnclaveEnvironment appCompute = EnclaveEnvironment.builder()
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
        EnclaveEnvironment postCompute = EnclaveEnvironment.builder()
                .name("post-compute")
                .mrenclave("mrEnclave3")
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
                .thenReturn(EnclaveEnvironments.builder()
                        .appCompute(appCompute)
                        .postCompute(postCompute)
                        .build());

        SpsSession actualSpsSession = gramineSessionService.generateSession(request);
        log.info(actualSpsSession.toString());
        Map<String, Object> actualJsonMap = new Yaml().load(actualSpsSession.toString());
        String expectedJsonString = FileHelper.readFile("src/test/resources/gramine-tee-session.json");
        Map<String, Object> expectedYmlMap = new Yaml().load(expectedJsonString);
        assertRecursively(expectedYmlMap, actualJsonMap);
    }
    // endregion
}