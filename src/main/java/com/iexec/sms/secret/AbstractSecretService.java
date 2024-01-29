/*
 * Copyright 2024-2024 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iexec.sms.secret;

import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractSecretService {

    private final ExpiringMap<String, Boolean> secretExistenceCache = ExpiringMap.builder()
            .expiration(1, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.CREATED)
            .build();


    /**
     * Caches the existence of the secret.
     *
     * @param parametersToConcatAsKey String Variable Arguments which will be concatenated to form the key
     */
    protected void putSecretExistenceInCache(String... parametersToConcatAsKey) {
        String key = generateCacheKey(parametersToConcatAsKey);
        log.debug("Put secret existence in cache[key:{}]", key);
        if (StringUtils.isNotBlank(key)) {
            secretExistenceCache.put(key, Boolean.TRUE);
        } else {
            //no strong coupling with cache, no exception handling
            log.debug("Key is NULL, can't ");
        }
    }

    /**
     * Look in the cache to see if the secret exists.
     *
     * @param parametersToConcatAsKey String Variable Arguments which will be concatenated to form the key
     * @return true if an entry was found in cache and false otherwise.
     */
    protected boolean lookSecretExistenceInCache(String... parametersToConcatAsKey) {
        String key = generateCacheKey(parametersToConcatAsKey);
        log.debug("Search secret existence in cache[key:{}]", key);
        if (StringUtils.isBlank(key)) {
            //no strong coupling with cache, no exception handling
            return false;
        }
        Boolean found = secretExistenceCache.get(key);
        if (found == null || !found) {
            log.debug("Secret existence was not found in cache[key:{}]", key);
            return false;
        }
        log.debug("Secret existence was found in cache[key:{}]", key);
        return true;
    }

    /**
     * Method to be implemented by child classes and used to define a prefix for the cache key
     *
     * @return A string to be used as a prefix to the cache key
     */
    protected abstract String getPrefixCacheKey();


    /**
     * Generate the key by concatenating the parameters to form the key.
     * The methoed calls the getPrefixCacheKey method, which child classes must implement.
     *
     * @param parameters String Variable Arguments which will be concatenated to form the key
     * @return The Key as String
     */
    private String generateCacheKey(String... parameters) {

        StringBuilder sb = new StringBuilder(getPrefixCacheKey());
        for (String parameter : parameters) {
            if (StringUtils.isNotBlank(parameter)) {
                sb.append("_").append(parameter);
            }
        }
        return sb.toString();
    }
}
