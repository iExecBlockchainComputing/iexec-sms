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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "tee.worker")
public class TeeWorkerPipelineConfiguration {
    @NotNull(message = "Pipelines configuration must be provided")
    private List<Pipeline> pipelines;

    @Data
    public static class Pipeline {
        @NotBlank(message = "Pipeline version must be provided")
        private String version;

        @NotNull(message = "Pre-compute configuration must be provided")
        private StageConfig preCompute;

        @NotNull(message = "Post-compute configuration must be provided")
        private StageConfig postCompute;
    }

    @Data
    public static class StageConfig {
        @NotBlank(message = "Image must be provided")
        private String image;

        @NotBlank(message = "Fingerprint must be provided")
        private String fingerprint;

        @NotNull(message = "Heap size must be provided")
        private DataSize heapSize;

        @NotBlank(message = "Entrypoint must be provided")
        private String entrypoint;
    }
}
