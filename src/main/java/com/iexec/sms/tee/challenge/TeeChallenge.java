package com.iexec.sms.tee.challenge;

import com.iexec.sms.utils.EthereumCredentials;
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
    private EthereumCredentials credentials;

    public TeeChallenge(String taskId) throws Exception {
        this.taskId = taskId;
        this.credentials = new EthereumCredentials();
    }
}
