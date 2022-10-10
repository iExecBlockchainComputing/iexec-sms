/*
 * Copyright 2022 IEXEC BLOCKCHAIN TECH
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

import com.iexec.common.tee.TeeFramework;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SconeServicesProperties extends TeeServicesProperties {
    private String lasImage;

    public SconeServicesProperties() {
        super(TeeFramework.SCONE);
    }

    public SconeServicesProperties(TeeAppProperties preComputeProperties,
                                   TeeAppProperties postComputeProperties,
                                   String lasImage) {
        super(TeeFramework.SCONE, preComputeProperties, postComputeProperties);
        this.lasImage = lasImage;
    }
}
