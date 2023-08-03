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

package com.iexec.sms.secret.web3;

import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.base.AbstractSecretController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/secrets/web3")
@CrossOrigin
public class Web3SecretController extends AbstractSecretController<
        Web3Secret,
        Web3SecretHeader,
        Web3SecretService> {

    protected Web3SecretController(Web3SecretService web3SecretService, AuthorizationService authorizationService) {
        super(web3SecretService, authorizationService);
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Void> isWeb3SecretSet(@RequestParam String secretAddress) {
        return isSecretSet(new Web3SecretHeader(secretAddress));
    }

    @PostMapping
    public ResponseEntity<String> addWeb3Secret(@RequestHeader String authorization,
                                                @RequestParam String secretAddress,
                                                @RequestBody String secretValue) {
        return addSecret(authorization, new Web3SecretHeader(secretAddress), secretValue);
    }

    @Override
    public String getChallenge(Web3SecretHeader header, String value) {
        return authorizationService.getChallengeForSetWeb3Secret(
                header.getAddress(),
                value
        );
    }

    @Override
    public boolean isCorrectlySigned(String challenge, String authorization, Web3SecretHeader header) {
        return authorizationService.isSignedByOwner(challenge, authorization, header.getAddress());
    }
}
