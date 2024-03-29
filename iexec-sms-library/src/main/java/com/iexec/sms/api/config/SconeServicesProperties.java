/*
 * Copyright 2022-2023 IEXEC BLOCKCHAIN TECH
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
public class SconeServicesProperties extends TeeServicesProperties {
    private final String lasImage;

    @JsonCreator
    public SconeServicesProperties(@JsonProperty("preComputeProperties") TeeAppProperties preComputeProperties,
                                   @JsonProperty("postComputeProperties") TeeAppProperties postComputeProperties,
                                   @JsonProperty("lasImage") String lasImage) {
        super(TeeFramework.SCONE, preComputeProperties, postComputeProperties);
        this.lasImage = lasImage;
    }
}
