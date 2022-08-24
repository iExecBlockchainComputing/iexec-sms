package com.iexec.sms.api;

import com.iexec.common.chain.IexecHubAbstractService;
import com.iexec.common.task.TaskDescription;
import feign.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

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

        final SmsClient smsClient = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_1));
        assertNotNull(smsClient);
    }

    @Test
    void shouldNotRebuildSmsClientForSameTask() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(TASK_DESCRIPTION_1);

        final SmsClient smsClient1 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_1));
        assertNotNull(smsClient1);

        final SmsClient smsClient2 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_1));
        assertNotNull(smsClient2);

        assertEquals(smsClient1, smsClient2);
    }

    @Test
    void shouldNotRebuildSmsClientForSameSmsUrlOnAnotherTask() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(TASK_DESCRIPTION_1);
        final SmsClient smsClient1 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_1));
        assertNotNull(smsClient1);

        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_3)).thenReturn(TASK_DESCRIPTION_3);
        final SmsClient smsClient2 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_3));
        assertNotNull(smsClient2);

        assertEquals(smsClient1, smsClient2);
    }

    @Test
    void shouldBuildAnotherSmsClientForTaskOnSecondCallForAnotherSms() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(TASK_DESCRIPTION_1);
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_2)).thenReturn(TASK_DESCRIPTION_2);

        final SmsClient smsClientForTask1 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_1));
        assertNotNull(smsClientForTask1);

        final SmsClient smsClientForTask2 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_2));
        assertNotNull(smsClientForTask2);


        assertNotEquals(smsClientForTask1, smsClientForTask2);
    }

    @Test
    void shouldNotGetSmsClientForTaskWhenNoTask() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(null);

        SmsClientCreationException e = assertThrows(SmsClientCreationException.class,
                () -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_1));
        assertEquals("No such task [chainTaskId: " + CHAIN_TASK_ID_1 +"]", e.getMessage());
    }

    @Test
    void shouldNotGetSmsClientForTaskWhenNoSmsUrl() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(TaskDescription.builder().build());

        SmsClientCreationException e = assertThrows(SmsClientCreationException.class,
                () -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_1));
        assertEquals("No SMS URL defined for given task [chainTaskId: " + CHAIN_TASK_ID_1 +"]", e.getMessage());
    }
}