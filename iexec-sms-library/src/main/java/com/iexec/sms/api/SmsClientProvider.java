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
     * Retrieves the SMS URL defined for this task and caches the result.
     * </p>
     * If the task has already been initialized, then gets the URL from the {@link TaskDescription}.
     * Otherwise, gets the URL from the {@link ChainDeal}.
     *
     * @param chainTaskId ID of the task the specified SMS URL should be retrieved.
     * @return The SMS URL defined for this task.
     */
    Optional<String> getSmsUrlForTask(String chainTaskId) {
        if (taskIdToSmsUrl.containsKey(chainTaskId)) {
            return taskIdToSmsUrl.get(chainTaskId);
        }

        final Optional<String> smsUrl;

        final TaskDescription taskDescription = iexecHubService.getTaskDescription(chainTaskId);
        if (taskDescription != null) {
            smsUrl = Optional.ofNullable(taskDescription.getSmsUrl());
        } else {
            // Fallback: if task is not initialized yet,
            // we can still get its SMS url in its deal.
            final Optional<ChainDeal> chainDeal = iexecHubService.getChainDeal(chainTaskId);
            smsUrl = chainDeal
                    .map(deal -> deal.getParams().getIexecSmsUrl());
        }

        taskIdToSmsUrl.put(chainTaskId, smsUrl);
        return smsUrl;
    }
}
