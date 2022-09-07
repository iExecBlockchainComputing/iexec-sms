package com.iexec.sms.api;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.DealParams;
import com.iexec.common.task.TaskDescription;
import feign.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;

class SmsClientProviderTests {
    private static final String CHAIN_DEAL_ID_1 = "chainDealId1";
    private static final String CHAIN_TASK_ID_1 = "chainTaskId1";
    private static final String SMS_URL_1 = "smsUrl1";
    private static final ChainDeal CHAIN_DEAL_1 = ChainDeal.builder()
            .params(DealParams.builder().iexecSmsUrl(SMS_URL_1).build())
            .build();
    private static final TaskDescription TASK_DESCRIPTION_1 = TaskDescription.builder()
            .chainTaskId(CHAIN_TASK_ID_1)
            .smsUrl(SMS_URL_1)
            .build();

    private static final String CHAIN_DEAL_ID_2 = "chainDealId2";
    private static final String CHAIN_TASK_ID_2 = "chainTaskId2";
    private static final String SMS_URL_2 = "smsUrl2";
    private static final ChainDeal CHAIN_DEAL_2 = ChainDeal.builder()
            .chainDealId(CHAIN_DEAL_ID_2)
            .params(DealParams.builder().iexecSmsUrl(SMS_URL_2).build())
            .build();
    private static final TaskDescription TASK_DESCRIPTION_2 = TaskDescription.builder()
            .chainTaskId(CHAIN_TASK_ID_2)
            .smsUrl(SMS_URL_2)
            .build();

    private static final String CHAIN_TASK_ID_3 = "chainTaskId3";
    private static final ChainDeal CHAIN_DEAL_3 = ChainDeal.builder()
            .params(DealParams.builder().iexecSmsUrl(SMS_URL_1).build())
            .build();
    private static final TaskDescription TASK_DESCRIPTION_3 = TaskDescription.builder()
            .chainTaskId(CHAIN_TASK_ID_3)
            .smsUrl(SMS_URL_1)
            .build();

    SmsClientProvider smsClientProvider;

    @BeforeEach
    void init()  {
        MockitoAnnotations.openMocks(this);
        smsClientProvider = spy(new SmsClientProvider(Logger.Level.NONE));
    }

    // region getOrCreateSmsClientForTask
    @Test
    void shouldGetSmsClientForTaskDescription() {
        final SmsClient smsClient = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(TASK_DESCRIPTION_1));
        assertNotNull(smsClient);
    }

    @Test
    void shouldNotRetrieveAgainSmsClientForSameTaskDescription() {
        final SmsClient smsClient1 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(TASK_DESCRIPTION_1));
        assertNotNull(smsClient1);

        final SmsClient smsClient2 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(TASK_DESCRIPTION_1));
        assertNotNull(smsClient2);

        assertEquals(smsClient1, smsClient2);
    }

    @Test
    void shouldNotRetrieveAgainSmsClientForSameSmsUrlOnAnotherTaskDescription() {
        final SmsClient smsClient1 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(TASK_DESCRIPTION_1));
        assertNotNull(smsClient1);

        final SmsClient smsClient2 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(TASK_DESCRIPTION_3));
        assertNotNull(smsClient2);

        assertEquals(smsClient1, smsClient2);
    }

    @Test
    void shouldRetrieveAnotherSmsClientForTaskDescriptionOnSecondCallForAnotherSms() {
        final SmsClient smsClientForTask1 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(TASK_DESCRIPTION_1));
        assertNotNull(smsClientForTask1);

        final SmsClient smsClientForTask2 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(TASK_DESCRIPTION_2));
        assertNotNull(smsClientForTask2);

        assertNotEquals(smsClientForTask1, smsClientForTask2);
    }

    @Test
    void shouldNotGetSmsClientForTaskDescriptionWhenNoSmsUrl() {
        final TaskDescription taskDescription = TaskDescription.builder().chainTaskId(CHAIN_TASK_ID_1).build();
        final SmsClientCreationException e = assertThrows(SmsClientCreationException.class,
                () -> smsClientProvider.getOrCreateSmsClientForTask(taskDescription));
        assertEquals("No SMS URL defined for given task [chainTaskId: " + CHAIN_TASK_ID_1 +"]", e.getMessage());
    }

    @Test
    void shouldNotGetSmsClientForTaskDescriptionWhenEmptySmsUrl() {
        final TaskDescription taskDescription = TaskDescription.builder().chainTaskId(CHAIN_TASK_ID_1).smsUrl("").build();
        final SmsClientCreationException e = assertThrows(SmsClientCreationException.class,
                () -> smsClientProvider.getOrCreateSmsClientForTask(taskDescription));
        assertEquals("No SMS URL defined for given task [chainTaskId: " + CHAIN_TASK_ID_1 + "]", e.getMessage());
    }
    // endregion

    // region getOrCreateSmsClientForUninitializedTask
    @Test
    void shouldGetSmsClientForUninitializedTask() {
        final SmsClient smsClient = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForUninitializedTask(CHAIN_DEAL_1, CHAIN_TASK_ID_1));
        assertNotNull(smsClient);
    }

    @Test
    void shouldNotRetrieveAgainSmsClientForSameDeal() {
        final SmsClient smsClient1 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForUninitializedTask(CHAIN_DEAL_1, CHAIN_TASK_ID_1));
        assertNotNull(smsClient1);

        final SmsClient smsClient2 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForUninitializedTask(CHAIN_DEAL_1, CHAIN_TASK_ID_1));
        assertNotNull(smsClient2);

        assertEquals(smsClient1, smsClient2);
    }

    @Test
    void shouldNotRetrieveAgainSmsClientForSameSmsUrlOnAnotherDeal() {
        final SmsClient smsClient1 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForUninitializedTask(CHAIN_DEAL_1, CHAIN_TASK_ID_1));
        assertNotNull(smsClient1);

        final SmsClient smsClient2 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForUninitializedTask(CHAIN_DEAL_3, CHAIN_TASK_ID_3));
        assertNotNull(smsClient2);

        assertEquals(smsClient1, smsClient2);
    }

    @Test
    void shouldRetrieveAnotherSmsClientForUninitializedTaskOnSecondCallForAnotherSms() {
        final SmsClient smsClientForUninitializedTask1 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForUninitializedTask(CHAIN_DEAL_1, CHAIN_TASK_ID_1));
        assertNotNull(smsClientForUninitializedTask1);

        final SmsClient smsClientForUninitializedTask2 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForUninitializedTask(CHAIN_DEAL_2, CHAIN_TASK_ID_2));
        assertNotNull(smsClientForUninitializedTask2);

        assertNotEquals(smsClientForUninitializedTask1, smsClientForUninitializedTask2);
    }

    @Test
    void shouldNotGetSmsClientForUninitializedTaskWhenNoSmsUrl() {
        final ChainDeal chainDeal = ChainDeal
                .builder()
                .chainDealId(CHAIN_DEAL_ID_1)
                .params(DealParams.builder().build())
                .build();
        final SmsClientCreationException e = assertThrows(SmsClientCreationException.class,
                () -> smsClientProvider.getOrCreateSmsClientForUninitializedTask(chainDeal, CHAIN_TASK_ID_1));
        assertEquals(
                "No SMS URL defined for given deal" +
                        " [chainDealId: " + CHAIN_DEAL_ID_1 + ", chainTaskId: " + CHAIN_TASK_ID_1 +"]",
                e.getMessage());
    }
    // endregion

    // region purgeTask
    @Test
    void shouldPurgeTask() {
        // Adding a task
        smsClientProvider.getOrCreateSmsClientForTask(TASK_DESCRIPTION_1);

        // Purging the task
        boolean purged = smsClientProvider.purgeTask(CHAIN_TASK_ID_1);
        assertTrue(purged);
    }

    @Test
    void shouldNotPurgeTaskSinceTaskNeverAccessed() {
        boolean purged = smsClientProvider.purgeTask(CHAIN_TASK_ID_1);
        assertFalse(purged);
    }
    // endregion
}