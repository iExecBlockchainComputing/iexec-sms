/*
 * Copyright 2022-2025 IEXEC BLOCKCHAIN TECH
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
 * To be used with {@link ConditionalOnTeeFramework}.
 */
@Slf4j
public class OnTeeFrameworkCondition extends SpringBootCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        final Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnTeeFramework.class.getName());
        final String[] activeProfiles = context.getEnvironment().getActiveProfiles();

        final String beanClassName;

        if (metadata instanceof AnnotationMetadata) {
            beanClassName = ((AnnotationMetadata) metadata).getClassName();
        } else {
            beanClassName = ((MethodMetadata) metadata).getMethodName();
        }

        if (attributes == null) {
            log.warn("No attribute for bean annotation, won't be loaded [bean:{}]",
                    beanClassName);
            return new ConditionOutcome(
                    false,
                    ConditionMessage.forCondition(ConditionalOnTeeFramework.class).didNotFind("any TEE frameworks").atAll());
        }

        final TeeFramework[] frameworks = (TeeFramework[]) attributes.get("frameworks");
        if (frameworks == null || frameworks.length == 0) {
            log.warn(
                    "No TEE framework defined for bean, won't be loaded [bean:{}]",
                    beanClassName);
            return new ConditionOutcome(
                    false,
                    ConditionMessage.forCondition(ConditionalOnTeeFramework.class).didNotFind("any TEE frameworks").atAll());
        }

        for (String activeProfile : activeProfiles) {
            for (TeeFramework framework : frameworks) {
                if (activeProfile.equalsIgnoreCase(framework.name())) {
                    return new ConditionOutcome(
                            true,
                            ConditionMessage.forCondition(ConditionalOnTeeFramework.class).foundExactly(framework));
                }
            }
        }

        log.debug(
                "Active profiles and condition don't match, bean won't be loaded [bean:{}]",
                beanClassName);

        return new ConditionOutcome(
                false,
                ConditionMessage.forCondition(ConditionalOnTeeFramework.class).didNotFind("profile", "profiles").items(Arrays.asList(frameworks)));
    }
}
