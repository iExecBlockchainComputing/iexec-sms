FROM nexus.iex.ec/sconecuratedimages-apps-java:jdk-alpine-scone3.0

ARG spring_boot_jar

RUN test -n "$spring_boot_jar"

COPY $spring_boot_jar                         /app/iexec-sms.jar
COPY build/resources/main/palaemonTemplate.vm /app/palaemonTemplate.vm
COPY src/main/resources/ssl-keystore-dev.p12 /app/ssl-keystore-dev.p12

# #### Docker ENV vars should be placed in palaemon conf for Scone ###

RUN SCONE_MODE=sim SCONE_HEAP=128M && \
    # This path must match the one in palaemon session.
    FSPF_PB_FILE=/fspf.pb && \
    # This path can be changed safely.
    FSPF_PB_FILE_KEYTAG=/fspf.keytag && \
    # This path can be change safely.
    FINGERPRINT_FILE=/fingerprint.txt && \
    # This value must match the heap size value
    # provided at runtime.
    RUNTIME_HEAP_SIZE=3G && \
    # Save the current file system state in fspf.pb file.
    scone fspf create ${FSPF_PB_FILE}                                           && \
    scone fspf addr ${FSPF_PB_FILE} /  --not-protected --kernel /               && \
    scone fspf addr ${FSPF_PB_FILE} /usr --authenticated --kernel /usr          && \
    scone fspf addf ${FSPF_PB_FILE} /usr /usr                                   && \
    scone fspf addr ${FSPF_PB_FILE} /bin --authenticated --kernel /bin          && \
    scone fspf addf ${FSPF_PB_FILE} /bin /bin                                   && \
    scone fspf addr ${FSPF_PB_FILE} /lib --authenticated --kernel /lib          && \
    scone fspf addf ${FSPF_PB_FILE} /lib /lib                                   && \
    scone fspf addr ${FSPF_PB_FILE} /etc/ssl --authenticated --kernel /etc/ssl  && \
    scone fspf addf ${FSPF_PB_FILE} /etc/ssl /etc/ssl                           && \
    scone fspf addr ${FSPF_PB_FILE} /sbin --authenticated --kernel /sbin        && \
    scone fspf addf ${FSPF_PB_FILE} /sbin /sbin                                 && \
    scone fspf addr ${FSPF_PB_FILE} /app --authenticated --kernel /app          && \
    scone fspf addf ${FSPF_PB_FILE} /app /app                                   && \
    # Encrypt fspf.pb file with a randomly generated key
    scone fspf encrypt ${FSPF_PB_FILE} > ${FSPF_PB_FILE_KEYTAG} && \
    # Get the runtime mrenclave
    MRENCLAVE="$(SCONE_HASH=1 SCONE_HEAP=${RUNTIME_HEAP_SIZE} java)" && \
    # Get fspf.pb file tag
    FSPF_TAG=$(awk '{print $9}' ${FSPF_PB_FILE_KEYTAG}) && \
    # Get fspf.pb file key
    FSPF_KEY=$(awk '{print $11}' ${FSPF_PB_FILE_KEYTAG}) && \
    # The complete fingerprint is composed of 3 parts
    FINGERPRINT="${FSPF_KEY}|${FSPF_TAG}|${MRENCLAVE}" && \
    echo ${FINGERPRINT} > ${FINGERPRINT_FILE} && \
    echo "<MRENCLAVE>${MRENCLAVE}</MRENCLAVE>" && \
    echo "<FSPF_KEY>${FSPF_KEY}</FSPF_KEY>" && \
    echo "<FSPF_TAG>${FSPF_TAG}</FSPF_TAG>" && \
    echo "<FINGERPRINT>${FINGERPRINT}</FINGERPRINT>"

# /!\ This should match the "command" entry in the palaemon config
ENTRYPOINT [ "/bin/sh", "-c", "java -jar /app/iexec-sms.jar" ]
