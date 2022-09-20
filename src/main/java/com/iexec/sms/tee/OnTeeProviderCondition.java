package com.iexec.sms.tee;

import com.iexec.common.tee.TeeEnclaveProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;

import java.util.Arrays;
import java.util.Map;

/**
 * {@link Condition} that checks for a specific profile to be enabled.
 * To be used with {@link ConditionalOnTeeProvider}.
 */
@Slf4j
public class OnTeeProviderCondition extends SpringBootCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        final Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnTeeProvider.class.getName());
        final String[] activeProfiles = context.getEnvironment().getActiveProfiles();

        final String beanClassName;

        if (metadata instanceof AnnotationMetadata) {
            beanClassName = ((AnnotationMetadata) metadata).getClassName();
        } else {
            beanClassName = ((MethodMetadata) metadata).getMethodName();
        }

        if (attributes == null) {
            log.warn("No attribute for bean annotation, won't be loaded [bean:{}",
                    beanClassName);
            return new ConditionOutcome(
                    false,
                    ConditionMessage.forCondition(ConditionalOnTeeProvider.class).didNotFind("any TEE enclave providers").atAll());
        }

        final TeeEnclaveProvider[] providers = (TeeEnclaveProvider[]) attributes.get("providers");
        if (providers == null || providers.length == 0) {
            log.warn(
                    "No TEE provider defined for bean, won't be loaded [bean:{}]",
                    beanClassName);
            return new ConditionOutcome(
                    false,
                    ConditionMessage.forCondition(ConditionalOnTeeProvider.class).didNotFind("any TEE enclave providers").atAll());
        }

        for (String activeProfile : activeProfiles) {
            for (TeeEnclaveProvider teeProvider : providers) {
                if (activeProfile.equalsIgnoreCase(teeProvider.name())) {
                    return new ConditionOutcome(
                            true,
                            ConditionMessage.forCondition(ConditionalOnTeeProvider.class).foundExactly(teeProvider));
                }
            }
        }

        log.debug(
                "Active profiles and condition don't match, bean won't be loaded [bean:{}]",
                beanClassName);

        return new ConditionOutcome(
                false,
                ConditionMessage.forCondition(ConditionalOnTeeProvider.class).didNotFind("profile", "profiles").items(Arrays.asList(providers)));
    }
}
