package com.iexec.sms.tee;

import com.iexec.common.tee.TeeEnclaveProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardMethodMetadata;

/**
 * Define a way to include beans only if a profile is enabled
 * <p/>
 * Usage:
 * Annotate any bean with the following:
 * {@code
 * @Conditional(EnableIfTeeProvider.class)
 * @EnableIfTeeProviderDefinition(providers = TeeEnclaveProvider.<member>)
 * }
 * <p/>
 * If bean is not annotated with {@link EnableIfTeeProviderDefinition}
 * or {@link EnableIfTeeProviderDefinition#providers()} is null or empty,
 * the bean won't be loaded.
 */
@Slf4j
public class EnableIfTeeProvider implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        final String[] activeProfiles = context.getEnvironment().getActiveProfiles();
        final String beanClassName = ((AnnotationMetadata) metadata).getClassName();
        final Class<?> clazz;
        try {
            clazz = Class.forName(beanClassName);
        } catch (ClassNotFoundException e) {
            log.error(
                    "Bean does not exist [bean: {}]",
                    beanClassName);
            return false;
        }

        final EnableIfTeeProviderDefinition condition =
                clazz.getAnnotation(EnableIfTeeProviderDefinition.class);
        if (condition == null) {
            log.error(
                    "@EnableIfTeeProviderDefinition annotation is required to use EnableIfTeeProvider condition [bean: {}]",
                    beanClassName);
            return false;
        }

        final TeeEnclaveProvider[] teeProviders = condition.providers();
        if (teeProviders == null || teeProviders.length == 0) {
            log.warn(
                    "No TEE provider defined for bean, won't be loaded [bean: {}]",
                    beanClassName);
            return false;
        }

        for (String activeProfile : activeProfiles) {
            for (TeeEnclaveProvider teeProvider : teeProviders) {
                if (activeProfile.equalsIgnoreCase(teeProvider.name())) {
                    return true;
                }
            }
        }

        log.debug(
                "Active profiles and condition don't match, bean won't be loaded [bean: {}]",
                beanClassName);
        return false;
    }
}
