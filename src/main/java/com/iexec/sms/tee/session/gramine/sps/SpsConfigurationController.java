/*
 * Copyright 2021 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.gramine.sps;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sps")
public class SpsConfigurationController {

    private final SpsConfiguration spsConfiguration;

    public SpsConfigurationController(SpsConfiguration spsConfiguration) {
        this.spsConfiguration = spsConfiguration;
    }

    /**
     * Get SPS public url intended for enclaves.
     * 
     * @return SPS url
     */
    @GetMapping("/url")
    public String getGramineSpsEnclaveUrl() {
        return spsConfiguration.getEnclaveUrl();
    }

}
