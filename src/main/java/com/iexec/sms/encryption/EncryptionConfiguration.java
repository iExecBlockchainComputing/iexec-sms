package com.iexec.sms.encryption;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EncryptionConfiguration {
    
    @Value("${encryption.aesKeyPath}")
    private String aesKeyPath;

}
