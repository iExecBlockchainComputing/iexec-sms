package com.iexec.sms.iexecsms.secret;

import lombok.*;
import org.springframework.data.annotation.Id;

@Data
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Secret {

    @Id
    private String id;
    private String address;
    private SecretPayload payload;

}
