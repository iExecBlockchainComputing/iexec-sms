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

package com.iexec.sms.tee.session.scone.cas;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SconeSession {

    @JsonProperty("name")
    private String name;
    @JsonProperty("version")
    private String version;
    @JsonProperty("access_policy")
    private AccessPolicy accessPolicy;
    @JsonProperty("services")
    private List<SconeEnclave> services;
    @JsonProperty("images")
    private List<Image> images;
    @JsonProperty("volumes")
    private List<Volumes> volumes;
    @JsonProperty("security")
    private Security security;

    @AllArgsConstructor
    @Getter
    public static class AccessPolicy {
        @JsonProperty("read")
        private List<String> read;
        @JsonProperty("update")
        private List<String> update;
    }

    @AllArgsConstructor
    public static class Image {
        @JsonProperty("name")
        private String name;
        @JsonProperty("volumes")
        private List<Volume> volumes;

        @Getter
        @AllArgsConstructor
        public static class Volume {
            @JsonProperty("name")
            private String name;
            @JsonProperty("path")
            private String path;
        }
    }

    @AllArgsConstructor
    public static class Volumes {
        @JsonProperty("name")
        private String name;
    }

    @AllArgsConstructor
    @Getter
    public static class Security {
        @JsonProperty("attestation")
        private Attestation attestation;

        public Security(List<String> tolerate, List<String> ignoreAdvisories) {
            this.attestation = new Attestation(tolerate, ignoreAdvisories);
        }

        @AllArgsConstructor
        @Getter
        public class Attestation {
            @JsonProperty("tolerate")
            private List<String> tolerate;
            @JsonProperty("ignore_advisories")
            private List<String> ignoreAdvisories;
        }
    }

    @Override
    public String toString() {
        try {
            return new YAMLMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Failed to write SPS session as string [session:{}]", name, e);
            return "";
        }
    }

}
