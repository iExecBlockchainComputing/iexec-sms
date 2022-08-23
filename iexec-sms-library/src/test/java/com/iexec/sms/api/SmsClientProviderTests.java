package com.iexec.sms.api;

import com.iexec.common.chain.IexecHubAbstractService;
import com.iexec.common.task.TaskDescription;
import feign.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SmsClientProviderTests {
    private static final String CHAIN_TASK_ID_1 = "chainTaskId1";
    private static final String SMS_URL_1 = "smsUrl1";
    private static final TaskDescription TASK_DESCRIPTION_1 = TaskDescription.builder()
            .chainTaskId(CHAIN_TASK_ID_1)
            .smsUrl(SMS_URL_1)
            .build();

    private static final String CHAIN_TASK_ID_2 = "chainTaskId2";
    private static final String SMS_URL_2 = "smsUrl2";
    private static final TaskDescription TASK_DESCRIPTION_2 = TaskDescription.builder()
            .chainTaskId(CHAIN_TASK_ID_2)
            .smsUrl(SMS_URL_2)
            .build();

    private static final String CHAIN_TASK_ID_3 = "chainTaskId1";
    private static final TaskDescription TASK_DESCRIPTION_3 = TaskDescription.builder()
            .chainTaskId(CHAIN_TASK_ID_3)
            .smsUrl(SMS_URL_1)  // Same as first task
            .build();

    @Mock
    IexecHubAbstractService iexecHubService;

    SmsClientProvider smsClientProvider;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        smsClientProvider = new SmsClientProvider(iexecHubService, Logger.Level.NONE);
    }

    @Test
    void shouldGetSmsClientForTask() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(TASK_DESCRIPTION_1);

        assertTrue(smsClientProvider.getSmsClientForTask(CHAIN_TASK_ID_1).isPresent());
    }

    @Test
    void shouldNotRebuildSmsClientForSameTask() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(TASK_DESCRIPTION_1);

        final Optional<SmsClient> smsClient1 = smsClientProvider.getSmsClientForTask(CHAIN_TASK_ID_1);
        assertTrue(smsClient1.isPresent());

        final Optional<SmsClient> smsClient2 = smsClientProvider.getSmsClientForTask(CHAIN_TASK_ID_1);
        assertTrue(smsClient2.isPresent());

        assertEquals(smsClient1, smsClient2);
    }

    @Test
    void shouldNotRebuildSmsClientForSameSmsUrlOnAnotherTask() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(TASK_DESCRIPTION_1);
        final Optional<SmsClient> smsClient1 = smsClientProvider.getSmsClientForTask(CHAIN_TASK_ID_1);
        assertTrue(smsClient1.isPresent());

        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_3)).thenReturn(TASK_DESCRIPTION_3);
        final Optional<SmsClient> smsClient2 = smsClientProvider.getSmsClientForTask(CHAIN_TASK_ID_3);
        assertTrue(smsClient2.isPresent());

        assertEquals(smsClient1, smsClient2);
    }

    @Test
    void shouldBuildAnotherSmsClientForTaskOnSecondCallForAnotherSms() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(TASK_DESCRIPTION_1);
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_2)).thenReturn(TASK_DESCRIPTION_2);

        final Optional<SmsClient> smsClientForTask1 = smsClientProvider.getSmsClientForTask(CHAIN_TASK_ID_1);
        assertTrue(smsClientForTask1.isPresent());

        final Optional<SmsClient> smsClientForTask2 = smsClientProvider.getSmsClientForTask(CHAIN_TASK_ID_2);
        assertTrue(smsClientForTask2.isPresent());

        assertNotEquals(smsClientForTask1, smsClientForTask2);
    }

    @Test
    void shouldNotGetSmsClientForTaskWhenNoTask() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(null);

        assertTrue(smsClientProvider.getSmsClientForTask(CHAIN_TASK_ID_1).isEmpty());
    }

    @Test
    void shouldNotGetSmsClientForTaskWhenNoSmsUrl() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(TaskDescription.builder().build());

        assertTrue(smsClientProvider.getSmsClientForTask(CHAIN_TASK_ID_1).isEmpty());
    }
}