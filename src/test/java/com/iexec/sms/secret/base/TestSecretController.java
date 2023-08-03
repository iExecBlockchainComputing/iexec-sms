/*
 * Copyright 2023-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.base;

import com.iexec.sms.authorization.AuthorizationService;

public class TestSecretController extends AbstractSecretController<
        TestSecret,
        TestSecretHeader,
        TestSecretService> {
    protected TestSecretController(TestSecretService secretService, AuthorizationService authorizationService) {
        super(secretService, authorizationService);
    }

    @Override
    public String getChallenge(TestSecretHeader header, String value) {
        return authorizationService.getChallengeForSetWeb3Secret(header.getId(), value);
    }

    @Override
    public boolean isCorrectlySigned(String challenge, String authorization, TestSecretHeader header) {
        return authorizationService.isSignedByHimself(challenge, authorization, header.getId());
    }
}
