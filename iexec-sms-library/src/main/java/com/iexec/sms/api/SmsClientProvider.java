package com.iexec.sms.api;

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
    private final Map<String, SmsClient> urlToSmsClient = new HashMap<>();

    private final IexecHubAbstractService iexecHubService;

    public SmsClientProvider(IexecHubAbstractService iexecHubService) {
        this.iexecHubService = iexecHubService;
    }

    /**
     * Retrieves the specified SMS URL for this task, then:
     * <ul>
     *     <li>If this SMS has already been accessed, returns the already-constructed {@link SmsClient};</li>
     *     <li>Otherwise, constructs, stores and returns a new {@link SmsClient}.</li>
     * </ul>
     *
     * @param chainTaskId ID of the task the specified SMS URL should be retrieved.
     * @return An instance of {@link SmsClient} pointing on the task's specified SMS.
     */
    public Optional<SmsClient> getSmsClientForTask(String chainTaskId) {
        final TaskDescription taskDescription = iexecHubService.getTaskDescription(chainTaskId);
        if (taskDescription == null) {
            log.warn("No such task [chainTaskId: {}]", chainTaskId);
            return Optional.empty();
        }

        final String smsUrl = taskDescription.getSmsUrl();
        if (StringUtils.isEmpty(smsUrl)) {
            log.warn("No SMS URL defined for given task [chainTaskId: {}]", chainTaskId);
            return Optional.empty();
        }

        return Optional.of(urlToSmsClient.computeIfAbsent(smsUrl, url -> SmsClientBuilder.getInstance(Logger.Level.NONE, url)));
    }
}
