package com.iexec.sms.iexecsms.tee.challenge;

import com.iexec.sms.iexecsms.utils.EthereumCredentials;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Getter
@Builder
@NoArgsConstructor
@Entity
@AllArgsConstructor
public class TeeChallenge {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    private String taskId;

    @OneToOne(cascade = {CascadeType.ALL})
    private EthereumCredentials credentials; //TODO encrypt tee challenge in DB
}
