#!/bin/bash
# IMG_FROM=docker.io/iexechub/iexec-sms:dev IMG_TO=docker.io/iexechub/iexec-sms:dev-debug ./sconify.sh
cd $(dirname $0)

SCONE_IMG_NAME=sconecuratedimages/iexec-sconify-image
SCONE_IMG_VERSION=5.7.0-wal

IMG_TO=${IMG_TO}-sconify-${SCONE_IMG_VERSION}-debug

ARGS=$(sed -e "s'\${IMG_FROM}'${IMG_FROM}'" -e "s'\${IMG_TO}'${IMG_TO}'" sconify.args)
echo $ARGS

SCONE_IMAGE="registry.scontain.com:5050/${SCONE_IMG_NAME}:${SCONE_IMG_VERSION}"

/bin/bash -c "docker run -t --rm \
    -v /var/run/docker.sock:/var/run/docker.sock \
    ${SCONE_IMAGE} \
        sconify_iexec \
            --cli=${SCONE_IMAGE} \
            --crosscompiler=${SCONE_IMAGE} \
            $ARGS"

echo
docker run --rm -e SCONE_HASH=1 $IMG_TO
