package com.iexec.sms.api;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.DealParams;
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
import static org.mockito.Mockito.*;

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

    private static final String CHAIN_TASK_ID_3 = "chainTaskId3";

    @Mock
    IexecHubAbstractService iexecHubService;

    SmsClientProvider smsClientProvider;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        smsClientProvider = spy(new SmsClientProvider(iexecHubService, Logger.Level.NONE));
    }

    // region getOrCreateSmsClientForTask
    @Test
    void shouldGetSmsClientForTask() {
        Mockito.when(smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_1)).thenReturn(Optional.of(SMS_URL_1));

        final SmsClient smsClient = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_1));
        assertNotNull(smsClient);
    }

    @Test
    void shouldNotRebuildSmsClientForSameTask() {
        Mockito.when(smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_1)).thenReturn(Optional.of(SMS_URL_1));

        final SmsClient smsClient1 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_1));
        assertNotNull(smsClient1);

        final SmsClient smsClient2 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_1));
        assertNotNull(smsClient2);

        assertEquals(smsClient1, smsClient2);
    }

    @Test
    void shouldNotRebuildSmsClientForSameSmsUrlOnAnotherTask() {
        Mockito.when(smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_1)).thenReturn(Optional.of(SMS_URL_1));
        final SmsClient smsClient1 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_1));
        assertNotNull(smsClient1);

        Mockito.when(smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_3)).thenReturn(Optional.of(SMS_URL_1));
        final SmsClient smsClient2 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_3));
        assertNotNull(smsClient2);

        assertEquals(smsClient1, smsClient2);
    }

    @Test
    void shouldBuildAnotherSmsClientForTaskOnSecondCallForAnotherSms() {
        Mockito.when(smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_1)).thenReturn(Optional.of(SMS_URL_1));
        Mockito.when(smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_2)).thenReturn(Optional.of(SMS_URL_2));

        final SmsClient smsClientForTask1 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_1));
        assertNotNull(smsClientForTask1);

        final SmsClient smsClientForTask2 = assertDoesNotThrow(() -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_2));
        assertNotNull(smsClientForTask2);

        assertNotEquals(smsClientForTask1, smsClientForTask2);
    }

    @Test
    void shouldNotGetSmsClientForTaskWhenNoSmsUrl() {
        Mockito.when(smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_1)).thenReturn(Optional.empty());

        SmsClientCreationException e = assertThrows(SmsClientCreationException.class,
                () -> smsClientProvider.getOrCreateSmsClientForTask(CHAIN_TASK_ID_1));
        assertEquals("No SMS URL defined for given task [chainTaskId: " + CHAIN_TASK_ID_1 +"]", e.getMessage());
    }
    // endregion

    // region getSmsUrlForTask
    @Test
    void shouldGetSmsUrlForTaskWhenTaskFound() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(TASK_DESCRIPTION_1);

        Optional<String> smsUrl = smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_1);
        assertTrue(smsUrl.isPresent());
        assertEquals(SMS_URL_1, smsUrl.get());

        verify(iexecHubService, times(1)).getTaskDescription(any());
        verify(iexecHubService, times(0)).getChainDeal(any());
    }

    @Test
    void shouldGetSmsUrlOnlyOnceForSameTask() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(TASK_DESCRIPTION_1);

        Optional<String> smsUrlFirstCall = smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_1);
        assertTrue(smsUrlFirstCall.isPresent());
        assertEquals(SMS_URL_1, smsUrlFirstCall.get());

        verify(iexecHubService, times(1)).getTaskDescription(any());
        verify(iexecHubService, times(0)).getChainDeal(any());

        Optional<String> smsUrlSecondCall = smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_1);
        assertTrue(smsUrlSecondCall.isPresent());
        assertEquals(SMS_URL_1, smsUrlSecondCall.get());

        verify(iexecHubService, times(1)).getTaskDescription(any());
        verify(iexecHubService, times(0)).getChainDeal(any());
    }

    @Test
    void shouldGetSmsUrlTwiceForDifferentTasks() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(TASK_DESCRIPTION_1);
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_2)).thenReturn(TASK_DESCRIPTION_2);

        Optional<String> smsUrlFirstCall = smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_1);
        assertTrue(smsUrlFirstCall.isPresent());
        assertEquals(SMS_URL_1, smsUrlFirstCall.get());

        verify(iexecHubService, times(1)).getTaskDescription(any());
        verify(iexecHubService, times(0)).getChainDeal(any());

        Optional<String> smsUrlSecondCall = smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_2);
        assertTrue(smsUrlSecondCall.isPresent());
        assertEquals(SMS_URL_2, smsUrlSecondCall.get());

        verify(iexecHubService, times(2)).getTaskDescription(any());
        verify(iexecHubService, times(0)).getChainDeal(any());
    }


    @Test
    void shouldGetSmsUrlForTaskWhenTaskNotFoundButDealFound() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(null);
        Mockito.when(iexecHubService.getChainDeal(CHAIN_TASK_ID_1)).thenReturn(Optional.of(
                ChainDeal.builder().params(DealParams.builder().iexecSmsUrl(SMS_URL_1).build()).build()
        ));

        Optional<String> smsUrl = smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_1);
        assertTrue(smsUrl.isPresent());
        assertEquals(SMS_URL_1, smsUrl.get());

        verify(iexecHubService, times(1)).getTaskDescription(any());
        verify(iexecHubService, times(1)).getChainDeal(any());
    }

    @Test
    void shouldNotGetSmsUrlForTaskWhenTaskAndDealNotFound() {
        Mockito.when(iexecHubService.getTaskDescription(CHAIN_TASK_ID_1)).thenReturn(null);
        Mockito.when(iexecHubService.getChainDeal(CHAIN_TASK_ID_1)).thenReturn(Optional.empty());

        Optional<String> smsUrl = smsClientProvider.getSmsUrlForTask(CHAIN_TASK_ID_1);
        assertTrue(smsUrl.isEmpty());

        verify(iexecHubService, times(1)).getTaskDescription(any());
        verify(iexecHubService, times(1)).getChainDeal(any());
    }
    // region
}