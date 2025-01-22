/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Value
@Validated
@ConfigurationProperties(prefix = "tee.worker")
public class TeeWorkerPipelineConfiguration {
    @NotEmpty(message = "Pipeline list must not be empty")
    List<@NotNull(message = "List elements must not be null") Pipeline> pipelines;

    @Value
    public static class Pipeline {
        @NotBlank(message = "Pipeline version must not be blank")
        String version;

        @NotNull(message = "Pre-compute configuration must not be null")
        @Valid
        StageConfig preCompute;

        @NotNull(message = "Post-compute configuration must not be null")
        @Valid
        StageConfig postCompute;
    }

    public record StageConfig(@NotBlank(message = "Image must not be blank") String image,
                                  @NotBlank(message = "Fingerprint must not be blank") String fingerprint,
                                  @NotNull(message = "Heap size must not be null") DataSize heapSize,
                                  @NotBlank(message = "Entrypoint must not be blank") String entrypoint) {
    }
}
