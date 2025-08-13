/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.scone;

import com.iexec.common.utils.FeignBuilder;
import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import com.iexec.sms.tee.session.base.SecretEnclaveBase;
import com.iexec.sms.tee.session.base.SecretSessionBase;
import com.iexec.sms.tee.session.base.SecretSessionBaseService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.scone.cas.SconeEnclave;
import com.iexec.sms.tee.session.scone.cas.SconeSession;
import com.iexec.sms.tee.session.scone.cas.SconeSession.AccessPolicy;
import com.iexec.sms.tee.session.scone.cas.SconeSession.Image;
import com.iexec.sms.tee.session.scone.cas.SconeSession.Image.Volume;
import com.iexec.sms.tee.session.scone.cas.SconeSession.Security;
import com.iexec.sms.tee.session.scone.cas.SconeSession.Volumes;
import feign.Logger;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

//TODO Rename and move
@Slf4j
@Service
@ConditionalOnTeeFramework(frameworks = TeeFramework.SCONE)
public class SconeSessionMakerService {

    private final SecretSessionBaseService secretSessionBaseService;
    private final SconeSessionSecurityConfig attestationSecurityConfig;
    private final Map<URI, AzureAttestationServer> azureAttestationServersMap;

    public SconeSessionMakerService(final SecretSessionBaseService secretSessionBaseService,
                                    final SconeSessionSecurityConfig attestationSecurityConfig) {
        this.secretSessionBaseService = secretSessionBaseService;
        this.attestationSecurityConfig = attestationSecurityConfig;
        azureAttestationServersMap = attestationSecurityConfig.getUrls().stream()
                .collect(Collectors.toMap(
                        url -> url,
                        url -> FeignBuilder.createBuilder(Logger.Level.BASIC).target(AzureAttestationServer.class, url.toString())
                ));
    }

    /**
     * Collect tokens required for different compute stages (pre, in, post)
     * and build the yaml config of the TEE session.
     * <p>
     * TODO: Read onchain available infos from enclave instead of copying
     * public vars to palaemon.yml. It needs ssl call from enclave to eth
     * node (only ethereum node address required inside palaemon.yml)
     *
     * @param request session request details
     * @return session config in yaml string format
     */
    @NonNull
    public SconeSession generateSession(final TeeSessionRequest request) throws TeeSessionGenerationException {
        final Volume iexecInVolume = new Volume("iexec_in", "/iexec_in");
        final Volume iexecOutVolume = new Volume("iexec_out", "/iexec_out");
        final Volume postComputeTmpVolume = new Volume("post-compute-tmp", "/post-compute-tmp");
        final List<SconeEnclave> services = new ArrayList<>();
        final List<Image> images = new ArrayList<>();

        final SecretSessionBase baseSession = secretSessionBaseService
                .getSecretsTokens(request);

        // pre (optional)
        if (baseSession.getPreCompute() != null) {
            final SconeEnclave sconePreEnclave = toSconeEnclave(
                    baseSession.getPreCompute(),
                    request.getTeeServicesProperties().getPreComputeProperties().getEntrypoint(),
                    true);
            services.add(sconePreEnclave);
            images.add(new Image(
                    sconePreEnclave.getImageName(),
                    List.of(iexecInVolume)));
        }
        // app
        final SconeEnclave sconeAppEnclave = toSconeEnclave(
                baseSession.getAppCompute(),
                request.getTaskDescription().getAppCommand(),
                false);
        services.add(sconeAppEnclave);
        images.add(new Image(
                sconeAppEnclave.getImageName(),
                List.of(iexecInVolume, iexecOutVolume)));
        // post
        final SconeEnclave sconePostEnclave = toSconeEnclave(
                baseSession.getPostCompute(),
                request.getTeeServicesProperties().getPostComputeProperties().getEntrypoint(),
                true);
        services.add(sconePostEnclave);
        images.add(new Image(
                sconePostEnclave.getImageName(),
                List.of(iexecOutVolume, postComputeTmpVolume)));

        final URI validAttestationServer = resolveValidAttestationServer();

        return SconeSession.builder()
                .name(request.getSessionId())
                .version("0.3.10")
                .accessPolicy(new AccessPolicy(List.of("CREATOR"), List.of("NONE")))
                .services(services)
                .images(images)
                .volumes(Arrays.asList(new Volumes(iexecInVolume.getName()),
                        new Volumes(iexecOutVolume.getName()),
                        new Volumes(postComputeTmpVolume.getName())))
                .security(
                        new Security(
                                attestationSecurityConfig.getToleratedInsecureOptions(),
                                attestationSecurityConfig.getIgnoredSgxAdvisories(),
                                attestationSecurityConfig.getMode(),
                                validAttestationServer
                        )
                )
                .build();
    }

    private URI resolveValidAttestationServer() {
        // The keys of the Map are shuffled to avoid always querying servers in the same order
        final List<URI> urls = new ArrayList<>(azureAttestationServersMap.keySet());
        Collections.shuffle(urls);
        for (final URI attestationServerUrl : urls) {
            try {
                azureAttestationServersMap.get(attestationServerUrl).canFetchOpenIdMetadata();
                log.debug("Resolved attestation server [url:{}]", attestationServerUrl);
                return attestationServerUrl;
            } catch (Exception e) {
                log.error("Failed to check Azure attestation server liveness [url:{}]", attestationServerUrl, e);
            }
        }
        return null;
    }

    private SconeEnclave toSconeEnclave(final SecretEnclaveBase enclaveBase,
                                        final String command,
                                        final boolean addJavaEnvVars) {
        final HashMap<String, Object> enclaveEnvironment = new HashMap<>(enclaveBase.getEnvironment());
        if (addJavaEnvVars) {
            enclaveEnvironment.putAll(
                    Map.of(
                            "LD_LIBRARY_PATH",
                            "/opt/java/openjdk/lib/server:/opt/java/openjdk/lib:/opt/java/openjdk/../lib",
                            "JAVA_TOOL_OPTIONS",
                            "-Xmx256m"
                    )
            );
        }
        return SconeEnclave.builder()
                .name(enclaveBase.getName())
                .imageName(enclaveBase.getName() + "-image")
                .mrenclaves(List.of(enclaveBase.getMrenclave()))
                .pwd("/")
                .command(command)
                .environment(enclaveEnvironment)
                .build();
    }

}
