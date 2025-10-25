#!/bin/bash

USERNAME="ubuntu"
HOST="${@: -2: 1}"
CMD="${@: -1: 1}"


declare -a OPTS
for SSH_ARG in "${@: 1: $# -3}" ; do
        if [[ "${SSH_ARG}" == --* ]] ; then
                OPTS+="${SSH_ARG} "
        fi
done

exec gcloud compute ssh $OPTS "${USERNAME}@${HOST}" -- -C "${CMD}"