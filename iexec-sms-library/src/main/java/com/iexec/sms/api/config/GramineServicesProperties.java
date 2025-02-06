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

package com.iexec.sms.api.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.iexec.commons.poco.tee.TeeFramework;
import lombok.Getter;

@Getter
public class GramineServicesProperties extends TeeServicesProperties {

    /**
     * GramineServicesProperties constructor.
     *
     * @deprecated This method is no longer acceptable to create a GramineServicesProperties object since we need the
     * TEE framework version also now.
     * Use {@link GramineServicesProperties(String, TeeAppProperties, TeeAppProperties)} instead.
     */
    @Deprecated(since = "8.7.0", forRemoval = true)
    public GramineServicesProperties(@JsonProperty("preComputeProperties") TeeAppProperties preComputeProperties,
                                     @JsonProperty("postComputeProperties") TeeAppProperties postComputeProperties) {
        super(TeeFramework.GRAMINE, "", preComputeProperties, postComputeProperties);
    }

    @JsonCreator
    public GramineServicesProperties(@JsonProperty("teeFrameworkVersion") String teeFrameworkVersion,
                                     @JsonProperty("preComputeProperties") TeeAppProperties preComputeProperties,
                                     @JsonProperty("postComputeProperties") TeeAppProperties postComputeProperties) {
        super(TeeFramework.GRAMINE, teeFrameworkVersion, preComputeProperties, postComputeProperties);
    }
}
