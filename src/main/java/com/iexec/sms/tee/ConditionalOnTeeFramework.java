package com.iexec.sms.tee;

import com.iexec.common.tee.TeeFramework;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a way to include beans based on active profiles.
 * <p>
 * E.g.:
 * <ul>
 *     <li>a {@code SconeConfig} bean annotated with {@code ConditionalOnTeeFramework(frameworks = SCONE)}
 *     will be loaded only if a {@code scone} profile is active.</li>
 *     <li>a {@code TeeConfig} bean annotated with {@code ConditionalOnTeeFramework(frameworks = {SCONE, GRAMINE})}
 *     will be loaded only if any of {@code scone} or {@code gramine} profile is active.</li>
 * </ul>
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnTeeFrameworkCondition.class)
public @interface ConditionalOnTeeFramework {
    TeeFramework[] frameworks();
}
