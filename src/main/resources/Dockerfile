# SCONE BASE
FROM sconecuratedimages/iexec:openjdk11-alpine3.10-test
COPY --from=sconecuratedimages/iexec:cli-alpine /opt/scone/scone-cli /opt/scone/scone-cli
COPY --from=sconecuratedimages/iexec:cli-alpine /usr/local/bin/scone /usr/local/bin/scone
COPY --from=sconecuratedimages/iexec:cli-alpine /opt/scone/bin /opt/scone/bin
RUN apk add bash --no-cache
RUN echo "http://dl-cdn.alpinelinux.org/alpine/v3.5/community" >> /etc/apk/repositories \
    && apk update

COPY build/resources/main/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

COPY build/resources/main/palaemonConfTemplateWithDataset.vm palaemonConfTemplateWithDataset.vm
COPY build/resources/main/palaemonConfTemplateWithoutDataset.vm palaemonConfTemplateWithoutDataset.vm

COPY build/libs/iexec-sms-@projectversion@.jar /app/iexec-sms.jar

COPY src/main/resources/ssl-keystore-dev.p12 /ssl-keystore-dev.p12

# /!\ Docker ENV vars should be placed in palaemon conf for Scone
# e.g.: ENV IEXEC_SMS_STORAGE_ENCRYPTION_AES_KEY_PATH /scone/iexec-sms-aes.key -> sms-palaemon-conf.yml

# SCONE FS PROTECTION
RUN SCONE_MODE=sim SCONE_HEAP=128M		    \
	&& mkdir conf							    \
	&& scone fspf create fspf.pb 					    \
	&& scone fspf addr fspf.pb /  --not-protected --kernel /            \
	&& scone fspf addr fspf.pb /usr --authenticated --kernel /usr       \
	&& scone fspf addf fspf.pb /usr /usr 			            \
	&& scone fspf addr fspf.pb /bin --authenticated --kernel /bin       \
	&& scone fspf addf fspf.pb /bin /bin 			            \
	&& scone fspf addr fspf.pb /lib --authenticated --kernel /lib       \
	&& scone fspf addf fspf.pb /lib /lib 			            \
	&& scone fspf addr fspf.pb /etc/ssl --authenticated --kernel /etc/ssl       \
	&& scone fspf addf fspf.pb /etc/ssl /etc/ssl 			            \
	&& scone fspf addr fspf.pb /sbin --authenticated --kernel /sbin     \
	&& scone fspf addf fspf.pb /sbin /sbin 			            \	    \
	&& scone fspf addr fspf.pb /app --authenticated --kernel /app 	    \
	&& scone fspf addf fspf.pb /app /app 				    \
	&& scone fspf encrypt ./fspf.pb > /conf/keytag 			    \
        && MRENCLAVE="$(SCONE_HASH=1 SCONE_HEAP=3G java)"			            \
	&& FSPF_TAG=$(cat conf/keytag | awk '{print $9}') 	            \
	&& FSPF_KEY=$(cat conf/keytag | awk '{print $11}')		    \
	&& FINGERPRINT="$FSPF_KEY|$FSPF_TAG|$MRENCLAVE"			    \
	&& echo $FINGERPRINT > conf/fingerprint.txt			    \
	&& printf "\n########################################################\nMREnclave: \n$FINGERPRINT\n########################################################\n\n"


ENTRYPOINT ["/app/entrypoint.sh"]

# docker run -it -p 15000:15000 nexus.iex.ec/iexec-sms:0.0.1