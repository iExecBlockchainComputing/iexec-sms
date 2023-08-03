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

package com.iexec.sms.secret.web2;

import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.SecretUtils;
import com.iexec.sms.secret.base.AbstractSecretController;
import com.iexec.sms.secret.exception.NotAnExistingSecretException;
import com.iexec.sms.secret.exception.SameSecretException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/secrets/web2")
@CrossOrigin
@Slf4j
public class Web2SecretController extends AbstractSecretController<
        Web2Secret,
        Web2SecretHeader,
        Web2SecretService> {
    protected Web2SecretController(Web2SecretService web2SecretService, AuthorizationService authorizationService) {
        super(web2SecretService, authorizationService);
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Void> isWeb2SecretSet(@RequestParam String ownerAddress,
                                                @RequestParam String secretName) {
        return isSecretSet(new Web2SecretHeader(ownerAddress, secretName));
    }

    @PostMapping
    public ResponseEntity<String> addWeb2Secret(@RequestHeader String authorization,
                                                @RequestParam String ownerAddress,
                                                @RequestParam String secretName,
                                                @RequestBody String secretValue) {
        return addSecret(authorization, new Web2SecretHeader(ownerAddress, secretName), secretValue);
    }

    @PutMapping
    public ResponseEntity<String> updateWeb2Secret(@RequestHeader String authorization,
                                                   @RequestParam String ownerAddress,
                                                   @RequestParam String secretName,
                                                   @RequestBody String newSecretValue) {
        final Web2SecretHeader header = new Web2SecretHeader(ownerAddress, secretName);
        log.trace("Updating secret [header:{}]", header);

        if (!SecretUtils.isSecretSizeValid(newSecretValue)) {
            log.trace("Failed to update secret: payload too large [header:{}]", header);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        String challenge = getChallenge(header, newSecretValue);

        if (!isCorrectlySigned(challenge, authorization, header)) {
            log.error("Unauthorized to updateSecret [header:{}, expectedChallenge:{}]", header, challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            secretService.updateSecret(header, newSecretValue);
            log.debug("Updated secret [header:{}]", header);
            return ResponseEntity.noContent().build();
        } catch (SameSecretException ignored) {
            log.error("Failed to update secret: secret has already this value [header:{}]", header);
            return ResponseEntity.noContent().build();
        } catch (NotAnExistingSecretException e) {
            log.error("Failed to update secret: secret does not exist yet [header:{}]", header);
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public String getChallenge(Web2SecretHeader header, String value) {
        return authorizationService.getChallengeForSetWeb2Secret(
                header.getOwnerAddress(),
                header.getAddress(),
                value
        );
    }

    @Override
    public boolean isCorrectlySigned(String challenge, String authorization, Web2SecretHeader header) {
        return authorizationService.isSignedByHimself(challenge, authorization, header.getOwnerAddress());
    }
}
