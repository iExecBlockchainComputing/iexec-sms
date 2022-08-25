package com.iexec.sms.api;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.IexecHubAbstractService;
import com.iexec.common.task.TaskDescription;
import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages the {@link SmsClient}, providing an easy way to access SMS
 * and avoiding the need to create a new {@link SmsClient} instance each time.
 */
@Slf4j
public class SmsClientProvider {
    // TODO: purge once task has been completed
    private final Map<String, Optional<String>> taskIdToSmsUrl = new HashMap<>();
    private final Map<String, SmsClient> urlToSmsClient = new HashMap<>();

    private final IexecHubAbstractService iexecHubService;
    private final Logger.Level loggerLevel;

    public SmsClientProvider(IexecHubAbstractService iexecHubService) {
        this.iexecHubService = iexecHubService;
        this.loggerLevel = Logger.Level.NONE;
    }

    public SmsClientProvider(IexecHubAbstractService iexecHubService,
                             Logger.Level loggerLevel) {
        this.iexecHubService = iexecHubService;
        this.loggerLevel = loggerLevel;
    }


    /**
     * Retrieves the specified SMS URL for this task, then:
     * <ul>
     *     <li>If this SMS has already been accessed, returns the already-constructed {@link SmsClient};</li>
     *     <li>Otherwise, constructs, stores and returns a new {@link SmsClient}.</li>
     * </ul>
     *
     * @param chainTaskId ID of the task the specified SMS URL should be retrieved.
     * @throws SmsClientCreationException if SMS URL can't be retrieved.
     * @return An instance of {@link SmsClient} pointing on the task's specified SMS.
     */
    public SmsClient getOrCreateSmsClientForTask(String chainTaskId) {
        final Optional<String> smsUrl = getSmsUrlForTask(chainTaskId);
        if (smsUrl.isEmpty() || StringUtils.isEmpty(smsUrl.get())) {
            throw new SmsClientCreationException("No SMS URL defined for given task [chainTaskId: " + chainTaskId +"]");
        }

        return urlToSmsClient.computeIfAbsent(smsUrl.get(), url -> SmsClientBuilder.getInstance(loggerLevel, url));
    }

    /**
     * Retrieves the SMS URL from the {@link TaskDescription} defined for this task and caches the result.
     *
     * @param chainTaskId ID of the task the specified SMS URL should be retrieved.
     * @return The SMS URL defined for this task.
     */
    Optional<String> getSmsUrlForTask(String chainTaskId) {
        if (taskIdToSmsUrl.containsKey(chainTaskId)) {
            return taskIdToSmsUrl.get(chainTaskId);
        }

        final TaskDescription taskDescription = iexecHubService.getTaskDescription(chainTaskId);
        final Optional<String> smsUrl = taskDescription != null
                ? Optional.ofNullable(taskDescription.getSmsUrl())
                : Optional.empty();
        taskIdToSmsUrl.put(chainTaskId, smsUrl);
        return smsUrl;
    }

    /**
     * Retrieves the specified SMS URL for this task based on its dealId, then:
     * <ul>
     *     <li>If this SMS has already been accessed, returns the already-constructed {@link SmsClient};</li>
     *     <li>Otherwise, constructs, stores and returns a new {@link SmsClient}.</li>
     * </ul>
     *
     * @param chainDealId ID of the task the specified SMS URL should be retrieved.
     * @param chainTaskId ID of the task the specified SMS URL should be stored.
     * @throws SmsClientCreationException if SMS URL can't be retrieved.
     * @return An instance of {@link SmsClient} pointing on the deal's specified SMS.
     */
    public SmsClient getOrCreateSmsClientForUninitializedTask(String chainDealId, String chainTaskId) {
        final Optional<String> smsUrl = getSmsUrlForUninitializedTask(chainDealId, chainTaskId);
        if (smsUrl.isEmpty() || StringUtils.isEmpty(smsUrl.get())) {
            throw new SmsClientCreationException("No SMS URL defined for given deal " +
                    "[chainDealId: " + chainDealId + ", chainTaskId: " + chainTaskId +"]");
        }

        return urlToSmsClient.computeIfAbsent(smsUrl.get(), url -> SmsClientBuilder.getInstance(loggerLevel, url));
    }

    /**
     * Retrieves the SMS URL from the {@link ChainDeal} defined for this task and caches the result.
     *
     * @param chainDealId ID of the deal the specified SMS URL should be retrieved.
     * @param chainTaskId ID of the task the specified SMS URL should be stored.
     * @return The SMS URL defined for this deal.
     */
    Optional<String> getSmsUrlForUninitializedTask(String chainDealId, String chainTaskId) {
        if (taskIdToSmsUrl.containsKey(chainTaskId)) {
            return taskIdToSmsUrl.get(chainTaskId);
        }

        final Optional<ChainDeal> chainDeal = iexecHubService.getChainDeal(chainDealId);
        final Optional<String> smsUrl = chainDeal
                .map(deal -> deal.getParams().getIexecSmsUrl());

        taskIdToSmsUrl.put(chainTaskId, smsUrl);
        return smsUrl;
    }

}
