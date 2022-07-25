package com.iexec.sms.tee.session.gramine;

import com.iexec.common.tee.TeeEnclaveConfiguration;
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
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import static com.iexec.sms.tee.session.TeeSessionTestUtils.*;
import static org.mockito.Mockito.*;

@Slf4j
class GramineSessionServiceTests {
    private static final String TEMPLATE_SESSION_FILE = "src/main/resources/gramineSessionTemplate.json.vm";
    private static final String EXPECTED_SESSION_FILE = "src/test/resources/gramine-tee-session.json";

    private static final TeeEnclaveConfiguration enclaveConfig =
            mock(TeeEnclaveConfiguration.class);

    @Mock
    private TeeWorkflowConfiguration teeWorkflowConfig;

    @Spy
    @InjectMocks
    private TeeSecretsService teeSecretsService;

    private GramineSessionMakerService gramineSessionService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);

        gramineSessionService = spy(new GramineSessionMakerService(teeSecretsService));
        ReflectionTestUtils.setField(gramineSessionService, "gramineTemplateFilePath", TEMPLATE_SESSION_FILE);
    }

    //region getSessionYml
    /**
     * FIXME
     * This is currently not a unit test.
     * It relies on {@link TeeSecretsService} implementation to work.
     * This should be fixed.
     */
    @Test
    void shouldGetSessionJson() throws Exception {
        TeeSecretsSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));

        doReturn(getPreComputeTokens()).when(teeSecretsService)
                .getPreComputeTokens(request);
        doReturn(getAppTokens()).when(teeSecretsService)
                .getAppTokens(request);
        doReturn(getPostComputeTokens()).when(teeSecretsService)
                .getPostComputeTokens(request);

        when(teeWorkflowConfig.getPreComputeEntrypoint()).thenReturn(PRE_COMPUTE_ENTRYPOINT);
        when(teeWorkflowConfig.getPostComputeEntrypoint()).thenReturn(POST_COMPUTE_ENTRYPOINT);

        SpsSession actualJsonString = gramineSessionService.generateSession(request);
        log.info(actualJsonString.toString());
/*
        Map<String, Object> actualJsonMap = new Yaml().load(actualJsonString);
        String expectedJsonString = FileHelper.readFile(EXPECTED_SESSION_FILE);
        Map<String, Object> expectedYmlMap = new Yaml().load(expectedJsonString);
        assertRecursively(expectedYmlMap, actualJsonMap);
        */
    }
    //endregion
}