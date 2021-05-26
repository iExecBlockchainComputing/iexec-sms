/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.workflow;

import com.iexec.common.tee.TeeWorkflowSharedConfiguration;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

@Configuration
@Getter
public class TeeWorkflowConfiguration {

    @Value("${tee.workflow.pre-compute.image}")
    @NotBlank(message = "pre-compute image must be provided")
    String preComputeImage;

    @Value("${tee.workflow.pre-compute.fingerprint}")
    @NotBlank(message = "pre-compute fingerprint must be provided")
    String preComputeFingerprint;

    @Value("${tee.workflow.pre-compute.heap-size}")
    @Positive(message = "pre-compute heap size must be provided")
    long preComputeHeapSize;
    
    @Value("${tee.workflow.post-compute.image}")
    @NotBlank(message = "post-compute image must be provided")
    String postComputeImage;
    
    @Value("${tee.workflow.post-compute.fingerprint}")
    @NotBlank(message = "post-compute fingerprint must be provided")
    String postComputeFingerprint;
    
    @Value("${tee.workflow.post-compute.heap-size}")
    @Positive(message = "post-compute heap size must be provided")
    long postComputeHeapSize;

    @Getter(AccessLevel.NONE) // no getter
    private Validator validator;

    public TeeWorkflowConfiguration(Validator validator) {
        this.validator = validator;
    }

    @PostConstruct
    private void validate() {
        if (!validator.validate(this).isEmpty()) {
            throw new ConstraintViolationException(validator.validate(this));
        }
    }

    public TeeWorkflowSharedConfiguration getPublicConfiguration() {
        return TeeWorkflowSharedConfiguration.builder()
                    .preComputeImage(preComputeImage)
                    .preComputeHeapSize(preComputeHeapSize)
                    .postComputeImage(postComputeImage)
                    .postComputeHeapSize(postComputeHeapSize)
                    .build();
    }
}
