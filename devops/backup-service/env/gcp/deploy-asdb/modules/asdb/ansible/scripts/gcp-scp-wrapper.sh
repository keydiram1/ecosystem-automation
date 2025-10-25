#!/bin/bash

USERNAME="ubuntu"
PROJECT="ecosystem-connectors-data"
ZONE=""
HOST="${@: -2:1}"
CMD="${@: -1:1}"


declare -a OPTS
for SCP_ARG in "${@:1:$#-3}"; do
    if [[ "${SCP_ARG}" == --zone* ]]; then
        ZONE="${SCP_ARG#--zone }"
        ZONE="${ZONE#--zone=}"
    elif [[ "${SCP_ARG}" == --* ]]; then
        OPTS+=("${SCP_ARG}")
    fi
done

CMD=$(echo "${CMD}" | tr -d [])

exec gcloud compute scp --project "$PROJECT" "${OPTS[@]}" "${USERNAME}@${HOST}" ${CMD}
