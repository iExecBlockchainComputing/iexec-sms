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

import java.util.concurrent.TimeUnit;

@Slf4j
public class CacheSecretService<K> {

    private final ExpiringMap<K, Boolean> secretExistenceCache = ExpiringMap.builder()
            .expiration(1, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.CREATED)
            .build();

    /**
     * Count how many entries are currently in the cache
     *
     * @return
     */
    public long count() {
        return secretExistenceCache.size();
    }

    /**
     * Reset entries in the cache
     */
    public void clear() {
        secretExistenceCache.clear();
    }

    /**
     * Caches the existence of the secret.
     *
     * @param key The key to use for cache
     */
    public void putSecretExistenceInCache(K key, boolean value) {
        log.debug("Put secret existence in cache[key:{}]", key);
        if (null != key) {
            secretExistenceCache.put(key, value);
        } else {
            //no strong coupling with cache, no exception handling
            log.warn("Key is NULL, unable to use cache");
        }
    }

    /**
     * Look in the cache to see if the secret exists.
     *
     * @param key The key to use for cache
     * @return true if an entry was found in cache and false otherwise.
     */
    public Boolean lookSecretExistenceInCache(K key) {
        log.debug("Search secret existence in cache[key:{}]", key);
        if (null == key) {
            //no strong coupling with cache, no exception handling
            log.warn("Key is NULL, unable to use cache");
            return false;
        }
        final Boolean found = secretExistenceCache.get(key);
        if (found == null) {
            log.debug("Secret existence was not found in cache[key:{}]", key);
        } else {
            log.debug("Secret existence was found in cache[key:{}, exist:{}]", key, found);
        }
        return found;
    }
}
