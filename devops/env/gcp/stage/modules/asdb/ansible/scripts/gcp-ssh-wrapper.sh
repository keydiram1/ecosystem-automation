#!/bin/bash

USERNAME="ubuntu"
PROJECT="ecosystem-connectors-data"
HOST="${@: -2: 1}"
CMD="${@: -1: 1}"


declare -a OPTS
for SSH_ARG in "${@: 1: $# -3}" ; do
        if [[ "${SSH_ARG}" == --* ]] ; then
                OPTS+="${SSH_ARG} "
        fi
done

exec gcloud compute ssh --project $PROJECT $OPTS "${USERNAME}@${HOST}" -- -C "${CMD}"
