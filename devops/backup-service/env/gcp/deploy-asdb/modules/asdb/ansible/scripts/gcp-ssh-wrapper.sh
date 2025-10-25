#!/bin/bash

USERNAME="ubuntu"
PROJECT="ecosystem-connectors-data"
ZONE=""
HOST="${@: -2:1}"
CMD="${@: -1:1}"

declare -a OPTS
for SSH_ARG in "${@:1:$#-3}"; do
    if [[ "${SSH_ARG}" == --zone* ]]; then
        ZONE="${SSH_ARG#--zone }"
        ZONE="${ZONE#--zone=}"
    elif [[ "${SSH_ARG}" == --* ]]; then
        OPTS+=("${SSH_ARG}")
    fi
done

if [[ -z "$ZONE" ]]; then
    echo "ERROR: No zone provided."
    exit 1
fi

exec gcloud compute ssh --project "$PROJECT" "${OPTS[@]}" "${USERNAME}@${HOST}" -- -C "${CMD}"
