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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.iexec.commons.poco.tee.TeeFramework;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "teeFramework", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "TDX", value = TdxServicesProperties.class),
        @JsonSubTypes.Type(name = "SCONE", value = SconeServicesProperties.class),
        @JsonSubTypes.Type(name = "GRAMINE", value = GramineServicesProperties.class)
})
public abstract class TeeServicesProperties {
    private final TeeFramework teeFramework;
    private final String teeFrameworkVersion;
    private final TeeAppProperties preComputeProperties;
    private final TeeAppProperties postComputeProperties;
}
