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

package com.iexec.sms;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "apiClient",
        url = ApiClient.API_URL)
public interface ApiClient {
    String API_URL = "http://localhost:${local.server.port}";

    @PostMapping("/apps/{appAddress}/secrets/{secretIndex}")
    ResponseEntity<String> addApplicationRuntimeSecret(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("appAddress") String appAddress,
            @PathVariable("secretIndex") long secretIndex,
            @RequestBody String secretValue
    );

    @RequestMapping(method = RequestMethod.HEAD, path = "/apps/{appAddress}/secrets/{secretIndex}")
    ResponseEntity<Void> isApplicationRuntimeSecretPresent(@PathVariable String appAddress,
                                                           @PathVariable long secretIndex);
}
