package com.iexec.sms.tee;

import com.iexec.common.tee.TeeEnclaveProvider;
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
        this.annotatedTypeScanner = new AnnotatedTypeScanner(true, ConditionalOnTeeProvider.class);
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
            final TeeEnclaveProvider[] providers = clazz.getAnnotation(ConditionalOnTeeProvider.class).providers();
            assertTrue(
                    areProfilesAndProvidersMatching(providers),
                    clazz.getName() + " should not have been loaded [profiles:" + Arrays.toString(activeProfiles) + "]"
            );
        }
    }

    private boolean areProfilesAndProvidersMatching(TeeEnclaveProvider[] providers) {
        for (TeeEnclaveProvider provider : providers) {
            for (String activeProfile : activeProfiles) {
                if (provider.name().equalsIgnoreCase(activeProfile)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Implement this method to check all beans of given TEE enclave provider are loaded (= not null).
     */
    abstract void checkTeeBeansAreLoaded();
}
