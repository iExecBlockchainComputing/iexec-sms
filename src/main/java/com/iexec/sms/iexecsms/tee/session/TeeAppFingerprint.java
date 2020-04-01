package com.iexec.sms.iexecsms.tee.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeeAppFingerprint {

    private String fspfKey;
    private String fspfTag;
    private String mrEnclave;
    private String entrypoint;

    public TeeAppFingerprint(String fingerprint, String splitter) throws IllegalArgumentException {
        String[] fields = fingerprint.split(splitter);

        if (fields.length < 3) {
            throw new IllegalArgumentException("Bad application fingerprint " + fingerprint);
        }

        this.fspfKey = fields[0];
        this.fspfTag = fields[1];
        this.mrEnclave = fields[2];

        if (fields.length > 3) {
            this.entrypoint = fields[3];
        } else {
            this.entrypoint = "";
        }
    }
}