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

package com.iexec.sms.secret;

import com.iexec.sms.utils.version.VersionService;
import org.springframework.stereotype.Service;

@Service
public class SecretUtils {
    private final VersionService versionService;

    public SecretUtils(VersionService versionService) {
        this.versionService = versionService;
    }

    public boolean isInProduction(String authorization) {
        boolean canAvoidAuthorization = versionService.isSnapshot() && authorization.equals("*");
        return !canAvoidAuthorization;
    }
}
