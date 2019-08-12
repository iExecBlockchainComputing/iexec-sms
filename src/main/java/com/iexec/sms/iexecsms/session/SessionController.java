package com.iexec.sms.iexecsms.session;


import com.iexec.common.sms.SmsRequest;
import com.iexec.common.sms.scone.SconeSecureSessionResponse;
import com.iexec.sms.iexecsms.PalaemonHelperService;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.StringWriter;
import java.util.Map;

@Slf4j
@RestController
public class SessionController {

    private PalaemonHelperService palaemonHelperService;

    private String PALAEMON_CONFIG_FILE_WITH_DATASET = "src/main/resources/palaemonConfTemplateWithDataset.vm";
    private String PALAEMON_CONFIG_FILE_WITHOUT_DATASET = "src/main/resources/palaemonConfTemplateWithoutDataset.vm";

    public SessionController(PalaemonHelperService palaemonHelperService) {
        this.palaemonHelperService = palaemonHelperService;
    }

    @PostMapping("/sessions/generate")
    public ResponseEntity<SconeSecureSessionResponse> generateSecureSession(@RequestBody SmsRequest smsRequest) throws Exception {

        // depends with dataSet or not

        // TODO contact the CAS and perform whatever is needed
        String taskId = smsRequest.getSmsSecretRequestData().getChainTaskId();
        String workerAddress = smsRequest.getSmsSecretRequestData().getWorkerAddress();

        // Palaemon file should be generated and a call to the CAS with this file should happen here.
        Map<String, String> tokens =palaemonHelperService.getTokenList(taskId, workerAddress);

        VelocityEngine ve = new VelocityEngine();
        ve.init();

        Template t = ve.getTemplate(PALAEMON_CONFIG_FILE_WITHOUT_DATASET);
        VelocityContext vc = new VelocityContext();
        // copy all data from the tokens into vc
        tokens.forEach(vc::put);

        StringWriter sw = new StringWriter();
        t.merge(vc, sw);

        // TODO: send all to the CAS to generate the session
        System.out.println(sw);
        SconeSecureSessionResponse data;
        data.getData().

        return ResponseEntity.notFound().build();
    }
}
