package com.iexec.sms.tee.session;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeeSessionHelperConfiguration {

    @Value("${scone.cas.palaemon}")
    private String palaemonTemplate;
}
