/*
 * Copyright 2020-2023 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.api.config.TeeServicesProperties;
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
import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;

//TODO Rename and move
@Service
@ConditionalOnTeeFramework(frameworks = TeeFramework.SCONE)
public class SconeSessionMakerService {

    private final SecretSessionBaseService secretSessionBaseService;
    private final TeeServicesProperties teeServicesConfig;
    private final SconeSessionSecurityConfig attestationSecurityConfig;

    public SconeSessionMakerService(
            SecretSessionBaseService secretSessionBaseService,
            TeeServicesProperties teeServicesConfig,
            SconeSessionSecurityConfig attestationSecurityConfig) {
        this.secretSessionBaseService = secretSessionBaseService;
        this.teeServicesConfig = teeServicesConfig;
        this.attestationSecurityConfig = attestationSecurityConfig;
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
    public SconeSession generateSession(TeeSessionRequest request)
            throws TeeSessionGenerationException, IllegalArgumentException, IllegalStateException {
        Volume iexecInVolume = new Volume("iexec_in", "/iexec_in");
        Volume iexecOutVolume = new Volume("iexec_out", "/iexec_out");
        Volume postComputeTmpVolume = new Volume("post-compute-tmp",
                "/post-compute-tmp");
        List<SconeEnclave> services = new ArrayList<>();
        List<Image> images = new ArrayList<>();

        SecretSessionBase baseSession = secretSessionBaseService
                .getSecretsTokens(request);

        // pre (optional)
        if (baseSession.getPreCompute() != null) {
            SconeEnclave sconePreEnclave = toSconeEnclave(
                    baseSession.getPreCompute(),
                    teeServicesConfig.getPreComputeProperties().getEntrypoint(),
                    true);
            services.add(sconePreEnclave);
            images.add(new Image(
                    sconePreEnclave.getImageName(),
                    List.of(iexecInVolume)));
        }
        // app
        SconeEnclave sconeAppEnclave = toSconeEnclave(
                baseSession.getAppCompute(),
                request.getTaskDescription().getAppCommand(),
                false);
        services.add(sconeAppEnclave);
        images.add(new Image(
                sconeAppEnclave.getImageName(),
                List.of(iexecInVolume, iexecOutVolume)));
        // post
        SconeEnclave sconePostEnclave = toSconeEnclave(
                baseSession.getPostCompute(),
                teeServicesConfig.getPostComputeProperties().getEntrypoint(),
                true);
        services.add(sconePostEnclave);
        images.add(new Image(
                sconePostEnclave.getImageName(),
                List.of(iexecOutVolume, postComputeTmpVolume)));

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
                                attestationSecurityConfig.getUrl()
                        )
                )
                .build();
    }

    private SconeEnclave toSconeEnclave(SecretEnclaveBase enclaveBase, String command, boolean addJavaEnvVars) {
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
