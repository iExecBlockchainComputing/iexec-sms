#!/bin/sh

cd ~/iexecdev/iexec-sms/src/main/resources/boot
docker rm -f iexec-sms

# Fresh CAS & Post session
(cd ~/iexecdev/iexec-deploy/core-dev; ./upstack)
curl -k -s --cert conf/client.crt --key conf/client-key.key --data-binary @sms-palaemon-conf.yml -X POST https://localhost:18767/session

# SMS boot
sudo rm -r /tmp/scone/*;
docker rm -f iexec-sms
docker-compose up