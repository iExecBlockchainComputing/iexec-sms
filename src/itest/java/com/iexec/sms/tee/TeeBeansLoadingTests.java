/*
 * Copyright 2022-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee;

import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.CommonTestSetup;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.util.AnnotatedTypeScanner;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Set;

import static com.iexec.sms.MockChainConfiguration.MOCK_CHAIN_PROFILE;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Subclass this class to test whether some TEE beans are correctly loaded.
 */
@Slf4j
@ActiveProfiles(MOCK_CHAIN_PROFILE)
public abstract class TeeBeansLoadingTests extends CommonTestSetup {

    final AnnotatedTypeScanner annotatedTypeScanner;
    final String[] activeProfiles;

    TeeBeansLoadingTests(@Autowired Environment environment) {
        this.annotatedTypeScanner = new AnnotatedTypeScanner(true, ConditionalOnTeeFramework.class);
        this.annotatedTypeScanner.setEnvironment(environment);
        this.activeProfiles = environment.getActiveProfiles();
    }

    /**
     * This will check that, for given active profiles,
     * we don't load beans that should be disabled.
     */
    @Test
    void checkNoUnwantedBeanIsLoaded() {
        final Set<Class<?>> sconeClasses = annotatedTypeScanner.findTypes("com", "iexec", "sms");
        for (Class<?> clazz : sconeClasses) {
            log.info("{} is loaded", clazz);
            final TeeFramework[] frameworks = clazz.getAnnotation(ConditionalOnTeeFramework.class).frameworks();
            assertTrue(
                    areProfilesAndFrameworksMatching(frameworks),
                    clazz.getName() + " should not have been loaded [profiles:" + Arrays.toString(activeProfiles) + "]"
            );
        }
    }

    private boolean areProfilesAndFrameworksMatching(TeeFramework[] frameworks) {
        for (TeeFramework framework : frameworks) {
            for (String activeProfile : activeProfiles) {
                if (framework.name().equalsIgnoreCase(activeProfile)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Implement this method to check all beans of given TEE framework are loaded (= not null).
     */
    abstract void checkTeeBeansAreLoaded();
}
