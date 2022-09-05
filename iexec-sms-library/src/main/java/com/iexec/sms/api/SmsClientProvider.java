package com.iexec.sms.api;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.task.TaskDescription;
import com.iexec.common.utils.purge.Purgeable;
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
public class SmsClientProvider implements Purgeable {
    private final Map<String, Optional<String>> taskIdToSmsUrl = new HashMap<>();
    private final Map<String, SmsClient> urlToSmsClient = new HashMap<>();

    private final Logger.Level loggerLevel;

    public SmsClientProvider() {
        this(Logger.Level.BASIC);
    }

    public SmsClientProvider(Logger.Level loggerLevel) {
        this.loggerLevel = loggerLevel;
    }

    /**
     * Retrieves the specified SMS URL for this task, then:
     * <ul>
     *     <li>If this SMS has already been accessed, returns the already-constructed {@link SmsClient};</li>
     *     <li>Otherwise, constructs, stores and returns a new {@link SmsClient}.</li>
     * </ul>
     *
     * @param taskDescription Task the specified SMS URL should be retrieved for.
     * @throws SmsClientCreationException if SMS URL can't be retrieved.
     * @return An instance of {@link SmsClient} pointing on the task's specified SMS.
     */
    public SmsClient getOrCreateSmsClientForTask(TaskDescription taskDescription) {
        final String chainTaskId = taskDescription.getChainTaskId();

        final Optional<String> smsUrl = taskIdToSmsUrl.computeIfAbsent(
                chainTaskId,
                id -> Optional.ofNullable(taskDescription.getSmsUrl())
        );

        if (smsUrl.isEmpty() || StringUtils.isEmpty(smsUrl.get())) {
            throw new SmsClientCreationException("No SMS URL defined for given task [chainTaskId: " + chainTaskId +"]");
        }

        return urlToSmsClient.computeIfAbsent(smsUrl.get(), url -> SmsClientBuilder.getInstance(loggerLevel, url));
    }

    /**
     * Retrieves the specified SMS URL for this task based on its dealId, then:
     * <ul>
     *     <li>If this SMS has already been accessed, returns the already-constructed {@link SmsClient};</li>
     *     <li>Otherwise, constructs, stores and returns a new {@link SmsClient}.</li>
     * </ul>
     *
     * @param deal Deal of the task the specified SMS URL should be retrieved.
     * @param chainTaskId ID of the task the specified SMS URL should be stored.
     * @throws SmsClientCreationException if SMS URL can't be retrieved.
     * @return An instance of {@link SmsClient} pointing on the deal's specified SMS.
     */
    public SmsClient getOrCreateSmsClientForUninitializedTask(ChainDeal deal, String chainTaskId) {
        final Optional<String> smsUrl = taskIdToSmsUrl.computeIfAbsent(
                chainTaskId,
                id -> Optional.ofNullable(deal.getParams().getIexecSmsUrl())
        );

        if (smsUrl.isEmpty() || StringUtils.isEmpty(smsUrl.get())) {
            throw new SmsClientCreationException("No SMS URL defined for given deal " +
                    "[chainDealId: " + deal.getChainDealId() + ", chainTaskId: " + chainTaskId +"]");
        }

        return urlToSmsClient.computeIfAbsent(smsUrl.get(), url -> SmsClientBuilder.getInstance(loggerLevel, url));
    }

    @Override
    public boolean purgeTask(String chainTaskId) {
        // Returning `taskIdToSmsUrl.remove(chainTaskId) != null` throws a warning:
        // don't compare an `Optional` with null.
        // So let's use a longer form.

        if (!taskIdToSmsUrl.containsKey(chainTaskId)) {
            return false;
        }
        taskIdToSmsUrl.remove(chainTaskId);
        return true;
    }
}
