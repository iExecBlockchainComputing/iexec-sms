package com.iexec.sms.tee;

import com.iexec.common.tee.TeeFramework;
import com.iexec.sms.tee.session.generic.TeeSessionHandler;
import com.iexec.sms.tee.session.gramine.GramineSessionHandlerService;
import com.iexec.sms.tee.session.gramine.GramineSessionMakerService;
import com.iexec.sms.tee.session.gramine.sps.SpsConfiguration;
import com.iexec.sms.tee.session.scone.SconeSessionHandlerService;
import com.iexec.sms.tee.session.scone.SconeSessionMakerService;
import com.iexec.sms.tee.session.scone.SconeSessionSecurityConfig;
import com.iexec.sms.tee.session.scone.cas.CasClient;
import com.iexec.sms.tee.session.scone.cas.CasConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ConditionalOnTeeFrameworkTests {
    @ConditionalOnTeeFramework(frameworks = {})
    static class NoFrameworksSet {}

    @Mock
    Environment environment;
    @Mock
    ConditionContext context;
    @Mock
    AnnotationMetadata metadata;
    OnTeeFrameworkCondition condition = new OnTeeFrameworkCondition();

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    static Stream<Arguments> sconeBeansClasses() {
        return Stream.of(
//                Arguments.of(SconeServicesProperties.class),  // Can't test this bean as it is declared through a method, not a class
                Arguments.of(SconeSessionHandlerService.class),
                Arguments.of(SconeSessionMakerService.class),
                Arguments.of(SconeSessionSecurityConfig.class),
                Arguments.of(CasClient.class),
                Arguments.of(CasConfiguration.class)
        );
    }

    static Stream<Arguments> gramineBeansClasses() {
        return Stream.of(
//                Arguments.of(GramineServicesProperties.class),    // Can't test this bean as it is declared through a method, not a class
                Arguments.of(GramineSessionHandlerService.class),
                Arguments.of(GramineSessionMakerService.class),
                Arguments.of(SpsConfiguration.class)
        );
    }

    static Stream<Arguments> sconeAndGramineBeansClasses() {
        return Stream.of(sconeBeansClasses(), gramineBeansClasses())
                .flatMap(Function.identity());
    }

    static Stream<Arguments> nonAnnotatedClasses() {
        return Stream.of(
                Arguments.of(TeeSessionHandler.class)
        );
    }

    // region shouldMatch
    @ParameterizedTest
    @MethodSource("sconeBeansClasses")
    void shouldMatchScone(Class<?> clazz) {
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"scone"});
        when(metadata.getClassName()).thenReturn(clazz.getName());
        setAttributesForMetadataMock(clazz);

        assertTrue(condition.matches(context, metadata));
    }

    @ParameterizedTest
    @MethodSource("gramineBeansClasses")
    void shouldMatchGramine(Class<?> clazz) {
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"gramine"});
        when(metadata.getClassName()).thenReturn(clazz.getName());
        setAttributesForMetadataMock(clazz);

        assertTrue(condition.matches(context, metadata));
    }

    @ParameterizedTest
    @MethodSource("sconeAndGramineBeansClasses")
    void shouldMatchSconeAndGramine(Class<?> clazz) {
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"scone", "gramine"});
        when(metadata.getClassName()).thenReturn(clazz.getName());
        setAttributesForMetadataMock(clazz);

        assertTrue(condition.matches(context, metadata));
    }
    // endregion

    // region shouldNotMatch
    @ParameterizedTest
    @MethodSource("sconeBeansClasses")
    void shouldNotMatchSconeSinceGramineProfile(Class<?> clazz) {
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"gramine"});
        when(metadata.getClassName()).thenReturn(clazz.getName());
        setAttributesForMetadataMock(clazz);

        assertFalse(condition.matches(context, metadata));
    }

    @ParameterizedTest
    @MethodSource("gramineBeansClasses")
    void shouldNotMatchGramineSinceSconeProfile(Class<?> clazz) {
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"scone"});
        when(metadata.getClassName()).thenReturn(clazz.getName());
        setAttributesForMetadataMock(clazz);

        assertFalse(condition.matches(context, metadata));
    }

    @ParameterizedTest
    @MethodSource("sconeAndGramineBeansClasses")
    void shouldNotMatchAnySinceNoProfile(Class<?> clazz) {
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        when(metadata.getClassName()).thenReturn(clazz.getName());
        setAttributesForMetadataMock(clazz);

        assertFalse(condition.matches(context, metadata));
    }

    @ParameterizedTest
    @MethodSource("nonAnnotatedClasses")
    void shouldNotMatchAnySinceNoAnnotation(Class<?> clazz) {
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        when(metadata.getClassName()).thenReturn(clazz.getName());

        assertFalse(condition.matches(context, metadata));
    }

    @Test
    void shouldNotMatchAnySinceNoFrameworkDefined() {
        final Class<?> clazz = NoFrameworksSet.class;

        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"scone", "gramine"});
        when(metadata.getClassName()).thenReturn(clazz.getName());
        setAttributesForMetadataMock(clazz);

        assertFalse(condition.matches(context, metadata));
    }

    @Test
    void shouldNotMatchAnySinceClassDoesNotExist() {
        final String className = "Not an existing class";

        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"scone", "gramine"});
        when(metadata.getClassName()).thenReturn(className);

        assertFalse(condition.matches(context, metadata));
    }
    // endregion

    void setAttributesForMetadataMock(Class<?> clazz) {
        ConditionalOnTeeFramework annotation = clazz.getAnnotation(ConditionalOnTeeFramework.class);
        TeeFramework[] frameworks = annotation.frameworks();
        when(metadata.getAnnotationAttributes(ConditionalOnTeeFramework.class.getName()))
                .thenReturn(Map.of("frameworks", frameworks));
    }
}
