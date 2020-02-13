#!/bin/sh
#cd ~/iexecdev/iexec-sms/src/main/resources/boot

#rm -r /tmp/scone/*;

docker network create iexec-net

docker rm -f iexec-cas iexec-las chain

docker-compose up -d iexec-cas iexec-las chain

sleep 15

curl -k -s --cert conf/client.crt --key conf/client-key.key --data-binary @sms-palaemon-conf.yml -X POST https://localhost:18767/session

docker-compose up iexec-sms